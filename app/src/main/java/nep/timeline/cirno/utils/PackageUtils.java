package nep.timeline.cirno.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Process;
import android.os.RemoteException;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

import com.topjohnwu.superuser.Shell;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.LinkedHashMap;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nep.timeline.cirno.CommonConstants;
import nep.timeline.cirno.binder.BinderService;
import nep.timeline.cirno.binders.ApplicationInterface;
import nep.timeline.cirno.binders.FrozenStateInterface;
import nep.timeline.cirno.configs.checkers.AppConfigs;
import nep.timeline.cirno.entity.AppItem;
import nep.timeline.cirno.entity.AppRecord;
import nep.timeline.cirno.entity.AppState;
import nep.timeline.cirno.log.Log;
import nep.timeline.cirno.provide.ApplicationBinder;
import nep.timeline.cirno.provide.FrozenStateBinder;
import nep.timeline.cirno.services.AppService;
import nep.timeline.cirno.virtuals.ProcessRecord;

public class PackageUtils {
    public static boolean isSystemUIChecker(Context context, PackageInfo packageInfo) {
        if (packageInfo == null || packageInfo.applicationInfo == null) {
            return false;
        }
        return PKGUtils.isSystemApp(packageInfo.applicationInfo);
    }

    public static List<AppItem> filter(int type) {
        List<AppItem> list = new ArrayList<>();
        Context context = nep.timeline.cirno.ui.utils.AppContext.INSTANCE.getContext();
        PackageManager pm = context.getPackageManager();
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        List<Integer> userIds = getInstalledUserIdsByPm();
        Log.d("App list users from pm: " + userIds);
        for (int userId : userIds) {
            List<String> packages = getInstalledPackagesForUserByPm(userId);
            Log.d("User #" + userId + " package count from pm: " + packages.size());
            int appended = 0;
            int fallback = 0;
            for (String pkg : packages) {
                if (pkg == null || pkg.isEmpty()) {
                    continue;
                }
                if (CommonConstants.isWhitelistApps(pkg)) {
                    continue;
                }
                ApplicationInfo info = getApplicationInfoAsUser(pm, pkg, userId);
                boolean system = PKGUtils.isSystemApp(info);
                if ((type == 1 && system) || (type == 2 && !system)) {
                    continue;
                }
                String appKey = pkg + "#" + userId;
                if (!seen.add(appKey)) {
                    continue;
                }

                AppItem item = new AppItem();
                item.packageName = pkg;
                item.userId = userId;
                try {
                    item.appName = String.valueOf(info.loadLabel(pm));
                    item.appIcon = info.loadIcon(pm);
                } catch (Throwable ignored) {
                    fallback++;
                    item.appName = pkg;
                    item.appIcon = new ColorDrawable(0x00000000);
                }
                try {
                    item.packageInfo = pm.getPackageInfo(pkg, PackageManager.GET_META_DATA);
                } catch (Throwable ignored) {
                    item.packageInfo = new PackageInfo();
                    item.packageInfo.packageName = pkg;
                }
                item.white = AppConfigs.isWhiteApp(pkg, item.userId);
                item.backgroundPlay = AppConfigs.isBackgroundPlayAllowed(pkg, item.userId);
                item.locationCheck = AppConfigs.isLocationUseAllowed(pkg, item.userId) ? 1 : 0;
                item.networkCheck = AppConfigs.isNetworkMessageAllowed(pkg, item.userId);
                item.socket = item.networkCheck;
                item.netReceive = item.networkCheck;
                list.add(item);
                appended++;
            }
            Log.d("User #" + userId + " app items appended: " + appended + ", fallback metadata: " + fallback);
        }

        list.sort(
            Comparator
                .comparing((AppItem item) -> !isConfigured(item))
                .thenComparing(item -> item.appName == null ? "" : item.appName.toLowerCase(Locale.ROOT))
        );

        return list;
    }

    private static boolean isConfigured(AppItem item) {
        return item.white || item.backgroundPlay || item.locationCheck != 0 || item.networkCheck;
    }

    private static List<Integer> getInstalledUserIdsByPm() {
        LinkedHashSet<Integer> userIds = new LinkedHashSet<>();
        Pattern userPattern = Pattern.compile("UserInfo\\{(\\d+):");
        List<String> lines = runRootCommand("pm list users");
        if (lines.isEmpty()) {
            lines = runRootCommand("cmd user list");
        }
        for (String line : lines) {
            Matcher matcher = userPattern.matcher(line);
            if (matcher.find()) {
                userIds.add(Integer.parseInt(matcher.group(1)));
            }
        }
        if (userIds.isEmpty()) {
            userIds.add(0);
        }
        return new ArrayList<>(userIds);
    }

    private static List<String> getInstalledPackagesForUserByPm(int userId) {
        LinkedHashSet<String> packages = new LinkedHashSet<>();
        List<String> lines = runRootCommand("pm list packages --user " + userId);
        if (lines.isEmpty()) {
            lines = runRootCommand("cmd package list packages --user " + userId);
        }
        for (String line : lines) {
            if (line == null || line.isEmpty()) {
                continue;
            }
            if (line.startsWith("package:")) {
                String pkg = line.substring("package:".length()).trim();
                if (!pkg.isEmpty()) {
                    packages.add(pkg);
                }
            }
        }
        return new ArrayList<>(packages);
    }

    private static List<String> runRootCommand(String command) {
        try {
            Shell.Result result = Shell.cmd(command).exec();
            if (result != null && result.isSuccess()) {
                return result.getOut();
            }
        } catch (Throwable ignored) {
        }
        return new ArrayList<>();
    }

    private static ApplicationInfo getApplicationInfoAsUser(PackageManager pm, String packageName, int userId) {
        try {
            Method method = pm.getClass().getMethod("getApplicationInfoAsUser", String.class, int.class, int.class);
            return (ApplicationInfo) method.invoke(pm, packageName, PackageManager.GET_META_DATA, userId);
        } catch (Throwable ignored) {
        }

        try {
            return pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
        } catch (Throwable ignored) {
        }

        ApplicationInfo info = new ApplicationInfo();
        info.packageName = packageName;
        info.enabled = true;
        info.uid = Process.FIRST_APPLICATION_UID;
        return info;
    }

    public static List<AppItem> getFrozenApplication(Context context) {
        List<AppItem> result = new ArrayList<>();
        if (context == null) {
            return result;
        }

        PackageManager pm = context.getPackageManager();
        ApplicationInterface applicationInterface = ApplicationBinder.getInstance();
        FrozenStateInterface frozenStateInterface = FrozenStateBinder.getInstance();
        if (applicationInterface == null || frozenStateInterface == null) {
            if (BinderService.binderCount() == 0) {
                nep.timeline.cirno.binder.BinderService.register(context);
                applicationInterface = ApplicationBinder.getInstance();
                frozenStateInterface = FrozenStateBinder.getInstance();
            }
        }
        if (applicationInterface == null || frozenStateInterface == null) {
            Log.i("Monitor data skipped: binder missing (Application=" + (applicationInterface != null) + ", FrozenState=" + (frozenStateInterface != null) + ")");
            return result;
        }

        LinkedHashSet<String> runningApps;
        try {
            List<String> running = applicationInterface.getRunningApplication();
            if (running == null || running.isEmpty()) {
                Log.i("Monitor data: running app list empty");
                return result;
            }
            runningApps = new LinkedHashSet<>(running);
            Log.i("Monitor data: running app entries=" + runningApps.size());
        } catch (RemoteException e) {
            Log.w("Monitor data failed: getRunningApplication", e);
            return result;
        }

        for (String entry : runningApps) {
            RunningApp runningApp = parseRunningApp(entry);
            if (runningApp == null) {
                continue;
            }

            ApplicationInfo applicationInfo = getApplicationInfoAsUser(pm, runningApp.packageName, runningApp.userId);
            if (applicationInfo == null) {
                continue;
            }

            Drawable icon;
            String appName;
            try {
                icon = applicationInfo.loadIcon(pm);
            } catch (Throwable ignored) {
                icon = new ColorDrawable(0x00000000);
            }
            try {
                appName = String.valueOf(applicationInfo.loadLabel(pm));
            } catch (Throwable ignored) {
                appName = runningApp.packageName;
            }

            PackageInfo packageInfo;
            try {
                packageInfo = getPackageInfoAsUser(pm, runningApp.packageName, runningApp.userId);
            } catch (Throwable ignored) {
                packageInfo = new PackageInfo();
                packageInfo.packageName = runningApp.packageName;
            }

            String frozenData;
            try {
                frozenData = frozenStateInterface.isFrozen(runningApp.packageName, runningApp.userId);
            } catch (RemoteException e) {
                continue;
            }
            FrozenSnapshot snapshot = parseFrozenSnapshot(frozenData);

            AppItem item = new AppItem();
            item.packageName = runningApp.packageName;
            item.userId = runningApp.userId;
            item.appName = appName;
            item.appIcon = icon;
            item.packageInfo = packageInfo;
            item.applicationProcessCount = snapshot.processCount;
            item.frozenProcessCount = snapshot.frozenCount;
            item.isFrozen = snapshot.isFrozen;
            item.frozenType = item.isFrozen ? "V2" : null;
            item.rss = snapshot.rss;
            item.notFrozenReason = item.isFrozen ? null : snapshot.reason;
            result.add(item);
        }

        return result;
    }

    private static PackageInfo getPackageInfoAsUser(PackageManager pm, String packageName, int userId) throws Exception {
        try {
            Method method = pm.getClass().getMethod("getPackageInfoAsUser", String.class, int.class, int.class);
            return (PackageInfo) method.invoke(pm, packageName, PackageManager.GET_META_DATA, userId);
        } catch (Throwable ignored) {
        }
        return pm.getPackageInfo(packageName, PackageManager.GET_META_DATA);
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
        return "UNKNOWN";
    }

    private static long readProcessRssKb(int pid) {
        if (pid <= 0) {
            return 0L;
        }

        Long direct = readRssFromStatusFile("/proc/" + pid + "/status");
        if (direct != null) {
            return direct;
        }

        List<String> out = runRootCommand("cat /proc/" + pid + "/status");
        for (String line : out) {
            if (line == null) {
                continue;
            }
            String trimmed = line.trim();
            if (!trimmed.startsWith("VmRSS:")) {
                continue;
            }
            String[] parts = trimmed.split("\\s+");
            if (parts.length >= 2) {
                try {
                    return Long.parseLong(parts[1]);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return 0L;
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

    private static RunningApp parseRunningApp(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        String[] split = value.split(":");
        if (split.length < 2) {
            return null;
        }
        try {
            return new RunningApp(split[0], Integer.parseInt(split[1]));
        } catch (NumberFormatException ignored) {
        }
        return null;
    }

    private static FrozenSnapshot parseFrozenSnapshot(String value) {
        FrozenSnapshot snapshot = new FrozenSnapshot();
        if (value == null || value.isEmpty()) {
            return snapshot;
        }
        snapshot.rss = parseIntBetween(value, "RSS[", "]", 0);
        if (value.startsWith("V2(")) {
            int slash = value.indexOf('/');
            int close = value.indexOf(')');
            if (slash > 3 && close > slash) {
                snapshot.frozenCount = parseIntSafe(value.substring(3, slash), 0);
                snapshot.processCount = parseIntSafe(value.substring(slash + 1, close), 0);
            }
            snapshot.isFrozen = snapshot.processCount > 0 && snapshot.frozenCount == snapshot.processCount;
            return snapshot;
        }

        snapshot.isFrozen = false;
        snapshot.reason = parseStringBetween(value, "NOT_FROZEN[", "]", "UNKNOWN");
        snapshot.processCount = parseIntBetween(value, "PROCESS_COUNT[", "]", 0);
        snapshot.frozenCount = parseIntBetween(value, "FROZEN_COUNT[", "]", 0);
        return snapshot;
    }

    private static String parseStringBetween(String source, String start, String end, String fallback) {
        int i = source.indexOf(start);
        if (i < 0) {
            return fallback;
        }
        int j = source.indexOf(end, i + start.length());
        if (j < 0) {
            return fallback;
        }
        return source.substring(i + start.length(), j);
    }

    private static int parseIntBetween(String source, String start, String end, int fallback) {
        String v = parseStringBetween(source, start, end, null);
        if (v == null) {
            return fallback;
        }
        return parseIntSafe(v, fallback);
    }

    private static int parseIntSafe(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
        }
        return fallback;
    }

    private static final class RunningApp {
        private final String packageName;
        private final int userId;

        private RunningApp(String packageName, int userId) {
            this.packageName = packageName;
            this.userId = userId;
        }
    }

    private static final class FrozenSnapshot {
        private boolean isFrozen;
        private String reason = "UNKNOWN";
        private int processCount;
        private int frozenCount;
        private long rss;
    }
}
