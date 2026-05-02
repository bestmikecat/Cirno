package nep.timeline.cirno.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

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
        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        for (ApplicationInfo info : apps) {
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
            AppItem item = new AppItem();
            item.packageName = pkg;
            item.userId = PKGUtils.getUserId(info.uid);
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

    public static List<AppItem> getFrozenApplication(Context context) {
        return new ArrayList<>();
    }
}
