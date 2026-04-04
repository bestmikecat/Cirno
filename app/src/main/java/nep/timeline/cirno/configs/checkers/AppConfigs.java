package nep.timeline.cirno.configs.checkers;

import nep.timeline.cirno.GlobalVars;
import nep.timeline.cirno.configs.settings.ApplicationSettings;

import java.util.HashSet;

public class AppConfigs {
    private static ApplicationSettings getSafeSettings() {
        if (GlobalVars.applicationSettings == null) {
            GlobalVars.applicationSettings = new ApplicationSettings();
        }
        if (GlobalVars.applicationSettings.whiteApps == null) {
            GlobalVars.applicationSettings.whiteApps = new HashSet<>();
        }
        if (GlobalVars.applicationSettings.backgroundPlayApps == null) {
            GlobalVars.applicationSettings.backgroundPlayApps = new HashSet<>();
        }
        if (GlobalVars.applicationSettings.locationUseApps == null) {
            GlobalVars.applicationSettings.locationUseApps = new HashSet<>();
        }
        return GlobalVars.applicationSettings;
    }

    public static boolean isWhiteApp(String pkg, int userId) {
        return getSafeSettings().whiteApps.contains(pkg + "#" + userId);
    }

    public static boolean isBackgroundPlayAllowed(String pkg, int userId) {
        return getSafeSettings().backgroundPlayApps.contains(pkg + "#" + userId);
    }

    public static boolean isLocationUseAllowed(String pkg, int userId) {
        return getSafeSettings().locationUseApps.contains(pkg + "#" + userId);
    }

    public static void setWhiteApp(String pkg, int userId, boolean enabled) {
        String key = pkg + "#" + userId;
        if (enabled) {
            getSafeSettings().whiteApps.add(key);
        } else {
            getSafeSettings().whiteApps.remove(key);
        }
    }

    public static void setBackgroundPlayAllowed(String pkg, int userId, boolean allowed) {
        String key = pkg + "#" + userId;
        if (allowed) {
            getSafeSettings().backgroundPlayApps.add(key);
        } else {
            getSafeSettings().backgroundPlayApps.remove(key);
        }
    }

    public static void setLocationUseAllowed(String pkg, int userId, boolean allowed) {
        String key = pkg + "#" + userId;
        if (allowed) {
            getSafeSettings().locationUseApps.add(key);
        } else {
            getSafeSettings().locationUseApps.remove(key);
        }
    }
}
