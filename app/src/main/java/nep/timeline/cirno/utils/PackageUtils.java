package nep.timeline.cirno.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Process;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

import com.topjohnwu.superuser.Shell;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nep.timeline.cirno.CommonConstants;
import nep.timeline.cirno.configs.checkers.AppConfigs;
import nep.timeline.cirno.entity.AppItem;
import nep.timeline.cirno.log.Log;

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
        return new ArrayList<>();
    }
}
