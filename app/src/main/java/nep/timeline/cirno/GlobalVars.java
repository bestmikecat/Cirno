package nep.timeline.cirno;

import nep.timeline.cirno.configs.settings.ApplicationSettings;
import nep.timeline.cirno.configs.settings.GlobalSettings;

public class GlobalVars {
    public static final String TAG = "Cirno";
    public static final String CONFIG = "Cirno";
    public final static String CONFIG_DIR = "/data/system/" + GlobalVars.CONFIG;
    public final static String LOG_DIR = CONFIG_DIR + "/log";
    public final static String BOOT_ID_SOURCE = "/proc/sys/kernel/random/boot_id";
    public final static String BOOT_ID_FILE = CONFIG_DIR + "/boot.id";
    public final static String ERROR_FLAG_FILE = CONFIG_DIR + "/error.flag";
    public static boolean isModuleActive = false;
    public static int XposedVersion = 0;
    public static ClassLoader classLoader;
    public static GlobalSettings globalSettings = null;
    public static ApplicationSettings applicationSettings = null;
}
