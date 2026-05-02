package nep.timeline.cirno.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.UserManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.lang.reflect.Method;

import nep.timeline.cirno.CommonConstants;
import nep.timeline.cirno.configs.checkers.AppConfigs;
import nep.timeline.cirno.entity.AppItem;

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
        for (int userId : getInstalledUserIds(context)) {
            for (ApplicationInfo info : getInstalledApplicationsForUser(pm, userId)) {
                if (info == null || !info.enabled) {
                    continue;
                }
                String pkg = info.packageName;
                if (CommonConstants.isWhitelistApps(pkg)) {
                    continue;
                }
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
                item.appName = String.valueOf(info.loadLabel(pm));
                Drawable icon = info.loadIcon(pm);
                item.appIcon = icon;
                try {
                    item.packageInfo = pm.getPackageInfo(pkg, PackageManager.GET_META_DATA);
                } catch (Throwable ignored) {
                    continue;
                }
                item.white = AppConfigs.isWhiteApp(pkg, item.userId);
                item.backgroundPlay = AppConfigs.isBackgroundPlayAllowed(pkg, item.userId);
                item.locationCheck = AppConfigs.isLocationUseAllowed(pkg, item.userId) ? 1 : 0;
                item.networkCheck = AppConfigs.isNetworkMessageAllowed(pkg, item.userId);
                item.socket = item.networkCheck;
                item.netReceive = item.networkCheck;
                list.add(item);
            }
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

    private static List<Integer> getInstalledUserIds(Context context) {
        LinkedHashSet<Integer> userIds = new LinkedHashSet<>();
        userIds.add(0);
        try {
            UserManager userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
            if (userManager == null) {
                return new ArrayList<>(userIds);
            }
            Method getUsersMethod = userManager.getClass().getMethod("getUsers");
            Object result = getUsersMethod.invoke(userManager);
            if (!(result instanceof List<?>)) {
                return new ArrayList<>(userIds);
            }
            List<?> users = (List<?>) result;
            for (Object user : users) {
                if (user == null) {
                    continue;
                }
                try {
                    Method getIdentifierMethod = user.getClass().getMethod("getUserHandle");
                    Object identifier = getIdentifierMethod.invoke(user);
                    if (identifier instanceof Integer) {
                        userIds.add((Integer) identifier);
                    }
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
        return new ArrayList<>(userIds);
    }

    @SuppressWarnings("unchecked")
    private static List<ApplicationInfo> getInstalledApplicationsForUser(PackageManager pm, int userId) {
        try {
            Method method = pm.getClass().getMethod("getInstalledApplicationsAsUser", int.class, int.class);
            Object result = method.invoke(pm, PackageManager.GET_META_DATA, userId);
            if (result instanceof List<?>) {
                return (List<ApplicationInfo>) result;
            }
        } catch (Throwable ignored) {
        }

        if (userId == 0) {
            return pm.getInstalledApplications(PackageManager.GET_META_DATA);
        }
        return new ArrayList<>();
    }

    public static List<AppItem> getFrozenApplication(Context context) {
        return new ArrayList<>();
    }
}
