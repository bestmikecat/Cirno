package nep.timeline.cirno.configs.checkers;

import nep.timeline.cirno.GlobalVars;
import nep.timeline.cirno.configs.policy.Capability;
import nep.timeline.cirno.configs.policy.PolicyKey;
import nep.timeline.cirno.configs.settings.ApplicationSettings;

import java.util.HashSet;
import java.util.Set;

public class AppConfigs {
    private static ApplicationSettings getSafeSettings() {
        if (GlobalVars.applicationSettings == null) {
            GlobalVars.applicationSettings = new ApplicationSettings();
        }
        if (GlobalVars.applicationSettings.backgroundPlayApps == null) {
            GlobalVars.applicationSettings.backgroundPlayApps = new HashSet<>();
        }
        if (GlobalVars.applicationSettings.locationUseApps == null) {
            GlobalVars.applicationSettings.locationUseApps = new HashSet<>();
        }
        if (GlobalVars.applicationSettings.whiteApps == null) {
            GlobalVars.applicationSettings.whiteApps = new HashSet<>();
        }
        return GlobalVars.applicationSettings;
    }

    private static Set<String> getCapabilityApps(Capability capability) {
        switch (capability) {
            case WHITE_LIST:
                return getSafeSettings().whiteApps;
            case ALLOW_BACKGROUND_AUDIO:
                return getSafeSettings().backgroundPlayApps;
            case ALLOW_LOCATION:
                return getSafeSettings().locationUseApps;
            default:
                throw new IllegalArgumentException("Unsupported capability: " + capability);
        }
    }

    public static boolean hasCapability(String pkg, int userId, Capability capability) {
        return getCapabilityApps(capability).contains(PolicyKey.of(pkg, userId));
    }

    public static void setCapability(String pkg, int userId, Capability capability, boolean enabled) {
        Set<String> apps = getCapabilityApps(capability);
        String key = PolicyKey.of(pkg, userId);
        if (enabled) {
            apps.add(key);
        } else {
            apps.remove(key);
        }
    }

    public static boolean isWhiteApp(String pkg, int userId) {
        return hasCapability(pkg, userId, Capability.WHITE_LIST);
    }

    public static boolean isBackgroundPlayAllowed(String pkg, int userId) {
        return hasCapability(pkg, userId, Capability.ALLOW_BACKGROUND_AUDIO);
    }

    public static boolean isLocationUseAllowed(String pkg, int userId) {
        return hasCapability(pkg, userId, Capability.ALLOW_LOCATION);
    }

    public static void setWhiteApp(String pkg, int userId, boolean enabled) {
        setCapability(pkg, userId, Capability.WHITE_LIST, enabled);
    }

    public static void setBackgroundPlayAllowed(String pkg, int userId, boolean allowed) {
        setCapability(pkg, userId, Capability.ALLOW_BACKGROUND_AUDIO, allowed);
    }

    public static void setLocationUseAllowed(String pkg, int userId, boolean allowed) {
        setCapability(pkg, userId, Capability.ALLOW_LOCATION, allowed);
    }
}
