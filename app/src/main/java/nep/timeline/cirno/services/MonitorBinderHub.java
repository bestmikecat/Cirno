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
import nep.timeline.cirno.binders.FrozenStateInterface;
import nep.timeline.cirno.configs.policy.FreezeExemption;
import nep.timeline.cirno.entity.AppRecord;
import nep.timeline.cirno.log.Log;
import nep.timeline.cirno.utils.FreezeExemptionChecker;
import nep.timeline.cirno.virtuals.ProcessRecord;

public final class MonitorBinderHub {
    private static final String REASON_UNKNOWN = "UNKNOWN";
    private static volatile long lastPublishedAtMs = 0L;
    private static volatile boolean bootCompleted = false;
    private static volatile List<String> cachedRunningApps = new ArrayList<>();
    private static final java.util.concurrent.ConcurrentHashMap<String, List<String>> PROCESS_NAME_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

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
        for (String key : PROCESS_NAME_CACHE.keySet()) {
            int separator = key.lastIndexOf('#');
            if (separator <= 0) {
                PROCESS_NAME_CACHE.remove(key);
                continue;
            }
            String packageName = key.substring(0, separator);
            int userId;
            try {
                userId = Integer.parseInt(key.substring(separator + 1));
            } catch (NumberFormatException e) {
                PROCESS_NAME_CACHE.remove(key);
                continue;
            }
            if (AppService.get(packageName, userId) == null) {
                PROCESS_NAME_CACHE.remove(key);
            }
        }
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
            String cacheKey = packageName + "#" + userId;
            List<String> cached = PROCESS_NAME_CACHE.get(cacheKey);
            if (cached != null) {
                return new Gson().toJson(cached);
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
            } catch (Throwable e) {
                Log.w("MonitorBinder getProcessesForApp failed pkg=" + packageName + " userId=" + userId, e);
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
            List<String> result = new ArrayList<>(processNames);
            PROCESS_NAME_CACHE.put(cacheKey, result);
            return new Gson().toJson(result);
        }

        @Override
        public String getNetworkSpeed(String packageName, int userId) {
            if (packageName == null || packageName.isEmpty()) {
                return "{\"rx\":0,\"tx\":0}";
            }
            AppRecord appRecord = AppService.get(packageName, userId);
            if (appRecord == null) {
                return "{\"rx\":0,\"tx\":0}";
            }
            long[] speed = NetworkSpeedMonitor.getSpeed(appRecord.getUid());
            return "{\"rx\":" + speed[0] + ",\"tx\":" + speed[1] + "}";
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
            float cpuUsage = 0f;
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
                processRecord.updateCachedCpuUsage();
                cpuUsage += processRecord.getCachedCpuUsage();
            }
            if (processCount <= 0) {
                return "NOT_FROZEN[UNKNOWN]";
            }
            String cpuString = String.format(java.util.Locale.ROOT, "%.2f", cpuUsage);
            if (frozenCount > 0) {
                return "V2(" + frozenCount + "/" + processCount + "),RSS[" + rss + "],CPU[" + cpuString + "]";
            }
            FreezeExemption exemption = FreezeExemptionChecker.check(appRecord);
            String reason;
            if (exemption != null) {
                reason = exemption.reason;
            } else if (frozenCount < processCount) {
                reason = "WAITING_FROZEN";
            } else {
                reason = REASON_UNKNOWN;
            }
            return "NOT_FROZEN[" + reason + "],PROCESS_COUNT[" + processCount + "],FROZEN_COUNT[" + frozenCount + "],RSS[" + rss + "],CPU[" + cpuString + "]";
        }

        @Override
        public List<String> getFrozenStates(List<String> apps) {
            List<String> result = new ArrayList<>();
            if (apps == null) {
                return result;
            }
            java.util.HashMap<String, String> localCache = new java.util.HashMap<>();
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
                String cacheKey = packageName + "#" + userId;
                String frozenState = localCache.get(cacheKey);
                if (frozenState == null) {
                    frozenState = isFrozen(packageName, userId);
                    localCache.put(cacheKey, frozenState);
                }
                result.add(frozenState);
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
            extras.putBinder("Status", StatusBinderHub.statusBinder);
            intent.putExtras(extras);
            ActivityManagerService.getContext().sendStickyBroadcast(intent);
            long delta = lastPublishedAtMs == 0L ? -1L : (now - lastPublishedAtMs);
            lastPublishedAtMs = now;
        } catch (Throwable e) {
            Log.w("MonitorBinder publish failed", e);
        }
    }

}
