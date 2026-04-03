package nep.timeline.cirno.configs.checkers;

import nep.timeline.cirno.GlobalVars;

public class AppConfigs {
    public static boolean isWhiteApp(String pkg, int userId) {
        return GlobalVars.applicationSettings.whiteApps.contains(pkg + "#" + userId);
    }

    public static boolean isBackgroundPlayAllowed(String pkg, int userId) {
        return GlobalVars.applicationSettings.backgroundPlayApps.contains(pkg + "#" + userId);
    }

    public static boolean isLocationUseAllowed(String pkg, int userId) {
        return GlobalVars.applicationSettings.locationUseApps.contains(pkg + "#" + userId);
    }

    public static void setBackgroundPlayAllowed(String pkg, int userId, boolean allowed) {
        String key = pkg + "#" + userId;
        if (allowed) {
            GlobalVars.applicationSettings.backgroundPlayApps.add(key);
        } else {
            GlobalVars.applicationSettings.backgroundPlayApps.remove(key);
        }
    }

    public static void setLocationUseAllowed(String pkg, int userId, boolean allowed) {
        String key = pkg + "#" + userId;
        if (allowed) {
            GlobalVars.applicationSettings.locationUseApps.add(key);
        } else {
            GlobalVars.applicationSettings.locationUseApps.remove(key);
        }
    }
}
