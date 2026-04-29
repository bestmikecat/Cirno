package nep.timeline.cirno.configs.checkers;

import nep.timeline.cirno.GlobalVars;
import nep.timeline.cirno.configs.policy.AppPolicy;
import nep.timeline.cirno.configs.policy.Capability;
import nep.timeline.cirno.configs.policy.PolicyKey;
import nep.timeline.cirno.configs.settings.ApplicationSettings;

import java.util.EnumSet;
import java.util.HashMap;

public class AppConfigs {
    private static ApplicationSettings getSafeSettings() {
        if (GlobalVars.applicationSettings == null) {
            GlobalVars.applicationSettings = new ApplicationSettings();
        }
        if (GlobalVars.applicationSettings.appPolicies == null) {
            GlobalVars.applicationSettings.appPolicies = new HashMap<>();
        }
        return GlobalVars.applicationSettings;
    }

    private static AppPolicy getOrCreatePolicy(String pkg, int userId) {
        String key = PolicyKey.of(pkg, userId);
        AppPolicy appPolicy = getSafeSettings().appPolicies.get(key);
        if (appPolicy == null) {
            appPolicy = new AppPolicy();
            getSafeSettings().appPolicies.put(key, appPolicy);
        }
        if (appPolicy.capabilities == null) {
            appPolicy.capabilities = EnumSet.noneOf(Capability.class);
        }
        return appPolicy;
    }

    public static boolean hasCapability(String pkg, int userId, Capability capability) {
        AppPolicy policy = getSafeSettings().appPolicies.get(PolicyKey.of(pkg, userId));
        return policy != null && policy.capabilities != null && policy.capabilities.contains(capability);
    }

    public static void setCapability(String pkg, int userId, Capability capability, boolean enabled) {
        AppPolicy policy = getOrCreatePolicy(pkg, userId);
        if (enabled) {
            policy.capabilities.add(capability);
        } else {
            policy.capabilities.remove(capability);
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
