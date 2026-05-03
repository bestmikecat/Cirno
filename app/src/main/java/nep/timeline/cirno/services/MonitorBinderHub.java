package nep.timeline.cirno.services;

import android.content.Intent;
import android.os.Bundle;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import nep.timeline.cirno.GlobalVars;
import nep.timeline.cirno.binders.ApplicationInterface;
import nep.timeline.cirno.binders.FrozenStateInterface;
import nep.timeline.cirno.configs.checkers.AppConfigs;
import nep.timeline.cirno.entity.AppRecord;
import nep.timeline.cirno.entity.AppState;
import nep.timeline.cirno.log.Log;
import nep.timeline.cirno.virtuals.ProcessRecord;

public final class MonitorBinderHub {
    private static final String REASON_UNKNOWN = "UNKNOWN";

    private MonitorBinderHub() {
    }

    private static final ApplicationInterface.Stub applicationBinder = new ApplicationInterface.Stub() {
        @Override
        public List<String> getRunningApplication() {
            List<String> result = new ArrayList<>();
            for (AppRecord appRecord : AppService.getAllRecordsSnapshot()) {
                if (appRecord == null) {
                    continue;
                }
                int processCount = 0;
                for (ProcessRecord processRecord : appRecord.getProcessRecords()) {
                    if (processRecord == null || processRecord.isDeathProcess()) {
                        continue;
                    }
                    processCount++;
                }
                if (processCount <= 0) {
                    continue;
                }
                result.add(appRecord.getPackageName() + ":" + appRecord.getUserId() + ":" + appRecord.getUid());
            }
            return result;
        }
    };

    private static final FrozenStateInterface.Stub frozenStateBinder = new FrozenStateInterface.Stub() {
        @Override
        public String isFrozen(String packageName, int userId) {
            if (packageName == null || packageName.isEmpty()) {
                return "NOT_FROZEN[UNKNOWN]";
            }
            AppRecord appRecord = AppService.get(packageName, userId);
            if (appRecord == null) {
                return "NOT_FROZEN[UNKNOWN]";
            }
            int processCount = 0;
            int frozenCount = 0;
            long rss = 0L;
            for (ProcessRecord processRecord : appRecord.getProcessRecords()) {
                if (processRecord == null || processRecord.isDeathProcess()) {
                    continue;
                }
                processCount++;
                if (processRecord.isFrozen()) {
                    frozenCount++;
                }
                rss += readProcessRssKb(processRecord.getPid());
            }
            if (processCount <= 0) {
                return "NOT_FROZEN[UNKNOWN]";
            }
            if (frozenCount == processCount) {
                return "V2(" + frozenCount + "/" + processCount + "),RSS[" + rss + "]";
            }
            String reason = resolveNotFrozenReason(appRecord, processCount, frozenCount);
            return "NOT_FROZEN[" + reason + "],PROCESS_COUNT[" + processCount + "],FROZEN_COUNT[" + frozenCount + "],RSS[" + rss + "]";
        }
    };

    public static void publish() {
        try {
            if (ActivityManagerService.instance == null || ActivityManagerService.getContext() == null) {
                Log.i("MonitorBinder publish skipped: AMS context not ready");
                return;
            }
            Intent intent = new Intent(GlobalVars.TAG + "-Binder");
            Bundle extras = new Bundle();
            extras.putBinder("Application", applicationBinder);
            extras.putBinder("FrozenState", frozenStateBinder);
            intent.putExtras(extras);
            ActivityManagerService.getContext().sendStickyBroadcast(intent);
            Log.i("MonitorBinder published: Application + FrozenState");
        } catch (Throwable ignored) {
            Log.w("MonitorBinder publish failed", ignored);
        }
    }

    private static String resolveNotFrozenReason(AppRecord appRecord, int processCount, int frozenProcessCount) {
        AppState appState = appRecord.getAppState();
        if (appState != null && appState.isVisible()) {
            return "VISIBLE";
        }
        if (AppConfigs.isWhiteApp(appRecord.getPackageName(), appRecord.getUserId())) {
            return "WHITELIST";
        }
        if (appState != null && AppConfigs.isBackgroundPlayAllowed(appRecord.getPackageName(), appRecord.getUserId()) && appState.isAudio()) {
            return "AUDIO";
        }
        if (appState != null && AppConfigs.isLocationUseAllowed(appRecord.getPackageName(), appRecord.getUserId()) && appState.isLocation()) {
            return "LOCATION";
        }
        if (appState != null && appState.isRecording()) {
            return "RECORDING";
        }
        if (appState != null && appState.isVpn()) {
            return "VPN";
        }
        if (frozenProcessCount > 0 && frozenProcessCount < processCount) {
            return "WAITING_FROZEN";
        }
        return REASON_UNKNOWN;
    }

    private static long readProcessRssKb(int pid) {
        if (pid <= 0) {
            return 0L;
        }
        Long direct = readRssFromStatusFile("/proc/" + pid + "/status");
        return direct == null ? 0L : direct;
    }

    private static Long readRssFromStatusFile(String path) {
        File file = new File(path);
        if (!file.exists() || !file.canRead()) {
            return null;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (!trimmed.startsWith("VmRSS:")) {
                    continue;
                }
                String[] parts = trimmed.split("\\s+");
                if (parts.length >= 2) {
                    return Long.parseLong(parts[1]);
                }
            }
        } catch (IOException | NumberFormatException ignored) {
        }
        return null;
    }
}
