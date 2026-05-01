package nep.timeline.cirno.log;

import android.os.Build;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.robv.android.xposed.XposedBridge;
import nep.timeline.cirno.GlobalVars;
import nep.timeline.cirno.configs.settings.GlobalSettings;
import nep.timeline.cirno.threads.Handlers;
import nep.timeline.cirno.utils.RWUtils;

public class Log {
    private final static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.JAPAN);
    private final static File currentLog = new File(GlobalVars.LOG_DIR, "current.log");

    static {
        i("设备Android SDK: " + Build.VERSION.SDK_INT);
    }

    static {
        i("冻结延时: " + 1000 * GlobalVars.globalSettings.freezeDelay + "ms");
    }

    public static void d(String msg) {
        execute("调试", msg);
    }

    public static void d(String msg, Throwable throwable) {
        d(msg + " 失败: " + throwable.getMessage());
    }

    public static void i(String msg) {
        execute("信息", msg);
    }

    public static void w(String msg) {
        execute("警告", msg);
    }

    public static void w(String msg, Throwable throwable) {
        w(msg + " 失败: " + throwable.getMessage());
    }

    public static void e(String msg) {
        execute("错误", msg);
    }

    public static void e(String msg, Throwable throwable) {
        e(msg + " 失败: " + throwable.getMessage());
    }

    private static boolean isLogEnabled() {
        return GlobalVars.globalSettings != null && GlobalVars.globalSettings.logEnabled;
    }

    private static String getLogOutputMode() {
        if (GlobalVars.globalSettings == null || GlobalVars.globalSettings.logOutputMode == null) {
            return GlobalSettings.LOG_OUTPUT_FILE;
        }
        return GlobalVars.globalSettings.logOutputMode;
    }

    public static void execute(String level, String msg) {
        if (!isLogEnabled()) {
            return;
        }

        String formatted = simpleDateFormat.format(new Date()) + " " + level.toUpperCase() + " -> " + msg;
        Handlers.log.post(() -> {
            if ("错误".equals(level)) {
                updateErrorFlag();
            }
            if (GlobalSettings.LOG_OUTPUT_FRAMEWORK.equals(getLogOutputMode())) {
                xposedLog(formatted);
            } else {
                fileLog(formatted);
            }
        });
    }

    public static void xposedLog(String msg) {
        XposedBridge.log(msg);
    }

    public static void fileLog(String msg) {
        try {
            RWUtils.writeStringToFile(currentLog, msg, true);
        } catch (IOException e) {
            xposedLog("Log write failed: " + e.getMessage() + " msg: " + msg);
        }
    }

    private static void updateErrorFlag() {
        try {
            String bootId = RWUtils.readConfig(GlobalVars.BOOT_ID_FILE).trim();
            if (!bootId.isEmpty()) {
                RWUtils.writeStringToFile(new File(GlobalVars.ERROR_FLAG_FILE), bootId, false);
            }
        } catch (IOException e) {
            xposedLog("Error flag write failed: " + e.getMessage());
        }
    }
}
