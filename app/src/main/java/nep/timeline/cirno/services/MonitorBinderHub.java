package nep.timeline.cirno.services;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.os.SystemClock;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import android.os.Parcel;

import com.google.gson.Gson;

import nep.timeline.cirno.GlobalVars;
import nep.timeline.cirno.binders.ApplicationInterface;
import nep.timeline.cirno.binders.ConfigInterface;
import nep.timeline.cirno.binders.FrozenStateInterface;
import nep.timeline.cirno.configs.checkers.AppConfigs;
import nep.timeline.cirno.entity.AppRecord;
import nep.timeline.cirno.entity.AppState;
import nep.timeline.cirno.log.Log;
import nep.timeline.cirno.utils.InputMethodData;
import nep.timeline.cirno.utils.PKGUtils;
import nep.timeline.cirno.virtuals.ProcessRecord;

public final class MonitorBinderHub {
    private static final String REASON_UNKNOWN = "UNKNOWN";
    private static volatile long lastPublishedAtMs = 0L;
    private static volatile boolean bootCompleted = false;
    private static volatile List<String> cachedRunningApps = new ArrayList<>();

    private MonitorBinderHub() {
    }

    public static void setBootCompleted() {
        bootCompleted = true;
    }

    public static void refreshRunningApps() {
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
        cachedRunningApps = result;
    }

    private static final ApplicationInterface.Stub applicationBinder = new ApplicationInterface.Stub() {
        @Override
        public List<String> getRunningApplication() {
            return cachedRunningApps;
        }

        @Override
        public String getProcessesForApp(String packageName, int userId) {
            if (packageName == null || packageName.isEmpty()) {
                return "[]";
            }
            LinkedHashSet<String> processNames = new LinkedHashSet<>();
            try {
                android.content.Context context = ActivityManagerService.getContext();
                if (context != null) {
                    PackageManager pm = context.getPackageManager();
                    if (pm != null) {
                        int flags = PackageManager.GET_ACTIVITIES | PackageManager.GET_SERVICES
                                | PackageManager.GET_RECEIVERS | PackageManager.GET_PROVIDERS;
                        PackageInfo pkgInfo = pm.getPackageInfo(packageName, flags);
                        if (pkgInfo.activities != null) {
                            for (ActivityInfo info : pkgInfo.activities) {
                                String name = info.processName;
                                if (name != null && !name.isEmpty()) {
                                    processNames.add(name);
                                }
                            }
                        }
                        if (pkgInfo.services != null) {
                            for (ServiceInfo info : pkgInfo.services) {
                                String name = info.processName;
                                if (name != null && !name.isEmpty()) {
                                    processNames.add(name);
                                }
                            }
                        }
                        if (pkgInfo.receivers != null) {
                            for (ActivityInfo info : pkgInfo.receivers) {
                                String name = info.processName;
                                if (name != null && !name.isEmpty()) {
                                    processNames.add(name);
                                }
                            }
                        }
                        if (pkgInfo.providers != null) {
                            for (ProviderInfo info : pkgInfo.providers) {
                                String name = info.processName;
                                if (name != null && !name.isEmpty()) {
                                    processNames.add(name);
                                }
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
            if (processNames.isEmpty()) {
                processNames.add(packageName);
            }
            AppRecord appRecord = AppService.get(packageName, userId);
            if (appRecord != null) {
                for (ProcessRecord processRecord : appRecord.getProcessRecords()) {
                    if (processRecord != null && !processRecord.isDeathProcess()) {
                        processNames.add(processRecord.getProcessName());
                    }
                }
            }
            return new Gson().toJson(new ArrayList<>(processNames));
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
                processRecord.updateCachedRss();
                rss += processRecord.getCachedRssKb();
            }
            if (processCount <= 0) {
                return "NOT_FROZEN[UNKNOWN]";
            }
            if (frozenCount > 0) {
                return "V2(" + frozenCount + "/" + processCount + "),RSS[" + rss + "]";
            }
            String reason = resolveNotFrozenReason(appRecord, processCount, frozenCount);
            return "NOT_FROZEN[" + reason + "],PROCESS_COUNT[" + processCount + "],FROZEN_COUNT[" + frozenCount + "],RSS[" + rss + "]";
        }

        @Override
        public List<String> getFrozenStates(List<String> apps) {
            List<String> result = new ArrayList<>();
            if (apps == null) {
                return result;
            }
            for (String entry : apps) {
                if (entry == null || entry.isEmpty()) {
                    result.add("");
                    continue;
                }
                String[] parts = entry.split(":");
                if (parts.length < 2) {
                    result.add("");
                    continue;
                }
                String packageName = parts[0];
                int userId;
                try {
                    userId = Integer.parseInt(parts[1]);
                } catch (NumberFormatException e) {
                    result.add("");
                    continue;
                }
                result.add(isFrozen(packageName, userId));
            }
            return result;
        }
    };

    @SuppressLint("MissingPermission")
    public static void publish() {
        publish("unspecified");
    }

    @SuppressLint("MissingPermission")
    public static void publish(String reason) {
        try {
            if (!bootCompleted) {
                return;
            }
            long now = SystemClock.uptimeMillis();
            if (ActivityManagerService.instance == null || ActivityManagerService.getContext() == null) {
                return;
            }
            Intent intent = new Intent(GlobalVars.TAG + "-Binder");
            Bundle extras = new Bundle();
            extras.putBinder("Application", applicationBinder);
            extras.putBinder("FrozenState", frozenStateBinder);
            extras.putBinder("Config", (ConfigInterface.Stub) ConfigBinderHub.configBinder);
            intent.putExtras(extras);
            ActivityManagerService.getContext().sendStickyBroadcast(intent);
            long delta = lastPublishedAtMs == 0L ? -1L : (now - lastPublishedAtMs);
            lastPublishedAtMs = now;
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
        if (AppConfigs.isBlackApp(appRecord.getPackageName(), appRecord.getUserId())) {
            return "BLACKLIST";
        }
        if (appRecord.equals(InputMethodData.currentInputMethodApp)) {
            return "INPUT";
        }
        if (PKGUtils.isSystemApp(appRecord.getApplicationInfo())) {
            return "SYSTEM";
        }
        if (appRecord.isWaitingNotification()) {
            return "WAITING_PUSH_RESPONSE";
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
        if (frozenProcessCount < processCount) {
            return "WAITING_FROZEN";
        }
        return REASON_UNKNOWN;
    }
}
