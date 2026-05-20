package nep.timeline.cirno.configs.settings;

public class GlobalSettings {
    public static final String LOG_OUTPUT_FRAMEWORK = "framework";
    public static final String LOG_OUTPUT_FILE = "file";
    public static final String LOG_LEVEL_NONE = "none";
    public static final String LOG_LEVEL_INFO = "info";
    public static final String LOG_LEVEL_DEBUG = "debug";

    public int netlinkUnit;
    public int freezeDelay = 5;
    public int wakeFreezeDelay = 30;
    public int networkSpeedThreshold = 102400;
    public String logLevel = LOG_LEVEL_INFO;
    public String logOutputMode = LOG_OUTPUT_FILE;

    public int navigationStyle;
    public boolean blurUI = true;
}
