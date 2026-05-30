package nep.timeline.cirno.configs.settings;

import java.util.HashSet;
import java.util.Set;

public class ApplicationSettings {
    public Set<String> blackApps = new HashSet<>();
    public Set<String> backgroundPlayApps = new HashSet<>();
    public Set<String> locationUseApps = new HashSet<>();
    public Set<String> whiteApps = new HashSet<>();
    public Set<String> networkMessageApps = new HashSet<>();
    public Set<String> networkSpeedApps = new HashSet<>();
    public Set<String> recordingApps = new HashSet<>();
    public Set<String> frozenProcessExclusions = new HashSet<>();

    public static ApplicationSettings ensureInitialized(ApplicationSettings settings) {
        ApplicationSettings target = settings == null ? new ApplicationSettings() : settings;

        if (target.blackApps == null) target.blackApps = new HashSet<>();
        if (target.backgroundPlayApps == null) target.backgroundPlayApps = new HashSet<>();
        if (target.locationUseApps == null) target.locationUseApps = new HashSet<>();
        if (target.whiteApps == null) target.whiteApps = new HashSet<>();
        if (target.networkMessageApps == null) target.networkMessageApps = new HashSet<>();
        if (target.networkSpeedApps == null) target.networkSpeedApps = new HashSet<>();
        if (target.recordingApps == null) target.recordingApps = new HashSet<>();
        if (target.frozenProcessExclusions == null) target.frozenProcessExclusions = new HashSet<>();

        return target;
    }
}
