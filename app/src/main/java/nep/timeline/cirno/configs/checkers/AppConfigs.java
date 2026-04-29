package nep.timeline.cirno.configs.checkers;

import nep.timeline.cirno.GlobalVars;
import nep.timeline.cirno.configs.policy.Capability;
import nep.timeline.cirno.configs.policy.PolicyKey;
import nep.timeline.cirno.configs.settings.ApplicationSettings;

import java.util.EnumSet;
import java.util.Set;

public class AppConfigs {
    private static ApplicationSettings getSafeSettings() {
        if (GlobalVars.applicationSettings == null) {
            GlobalVars.applicationSettings = new ApplicationSettings();
        }
        return GlobalVars.applicationSettings;
    }

    private static Set<Capability> getOrCreatePolicy(String pkg, int userId) {
        String key = PolicyKey.of(pkg, userId);
        Set<Capability> capabilities = getSafeSettings().get(key);
        if (capabilities == null) {
            capabilities = EnumSet.noneOf(Capability.class);
            getSafeSettings().put(key, capabilities);
        }
        return capabilities;
    }

    public static boolean hasCapability(String pkg, int userId, Capability capability) {
        Set<Capability> policy = getSafeSettings().get(PolicyKey.of(pkg, userId));
        return policy != null && policy.contains(capability);
    }

    public static void setCapability(String pkg, int userId, Capability capability, boolean enabled) {
        Set<Capability> policy = getOrCreatePolicy(pkg, userId);
        if (enabled) {
            policy.add(capability);
        } else {
            policy.remove(capability);
        }

        if (policy.isEmpty()) {
            getSafeSettings().remove(PolicyKey.of(pkg, userId));
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
