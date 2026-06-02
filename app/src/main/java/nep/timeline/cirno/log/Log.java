package nep.timeline.cirno.log;

import android.os.Build;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.robv.android.xposed.XposedBridge;
import nep.timeline.cirno.BuildConfig;
import nep.timeline.cirno.GlobalVars;
import nep.timeline.cirno.configs.settings.GlobalSettings;
import nep.timeline.cirno.services.StatusBinderHub;
import nep.timeline.cirno.threads.Handlers;
import nep.timeline.cirno.utils.RWUtils;

public class Log {
    private final static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.JAPAN);
    private final static File currentLog = new File(GlobalVars.LOG_DIR, "current.log");

    static {
        i("当前Cirno版本: v" + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + "-" + BuildConfig.BUILD_TIME + ")");
        i("设备Android SDK: " + Build.VERSION.SDK_INT);
    }

    static {
        int freezeDelay = 5;
        if (GlobalVars.globalSettings != null) {
            freezeDelay = GlobalVars.globalSettings.freezeDelay;
        }
        i("冻结延时: " + 1000 * freezeDelay + "ms");
    }

    public static void d(String msg) {
        execute("调试", msg);
    }

    public static void d(String msg, Throwable throwable) {
        if (throwable instanceof java.lang.reflect.InvocationTargetException) {
            Throwable target = ((java.lang.reflect.InvocationTargetException) throwable).getTargetException();
            if (target != null) {
                throwable = target;
            }
        }
        String detail = throwable.getClass().getSimpleName();
        String message = throwable.getMessage();
        if (message != null && !message.isEmpty()) {
            detail += ": " + message;
        }
        d(msg + " 失败: " + detail);
    }

    public static void i(String msg) {
        execute("信息", msg);
    }

    public static void w(String msg) {
        execute("警告", msg);
    }

    public static void w(String msg, Throwable throwable) {
        if (throwable instanceof java.lang.reflect.InvocationTargetException) {
            Throwable target = ((java.lang.reflect.InvocationTargetException) throwable).getTargetException();
            if (target != null) {
                throwable = target;
            }
        }
        String detail = throwable.getClass().getSimpleName();
        String message = throwable.getMessage();
        if (message != null && !message.isEmpty()) {
            detail += ": " + message;
        }
        w(msg + " 失败: " + detail);
        XposedBridge.log(throwable);
    }

    public static void e(String msg) {
        execute("错误", msg);
    }

    public static void e(String msg, Throwable throwable) {
        if (throwable instanceof java.lang.reflect.InvocationTargetException) {
            Throwable target = ((java.lang.reflect.InvocationTargetException) throwable).getTargetException();
            if (target != null) {
                throwable = target;
            }
        }
        String detail = throwable.getClass().getSimpleName();
        String message = throwable.getMessage();
        if (message != null && !message.isEmpty()) {
            detail += ": " + message;
        }
        e(msg + " 失败: " + detail);
        XposedBridge.log(throwable);
    }

    private static String getLogLevel() {
        if (GlobalVars.globalSettings == null || GlobalVars.globalSettings.logLevel == null) {
            return GlobalSettings.LOG_LEVEL_INFO;
        }
        return GlobalVars.globalSettings.logLevel;
    }

    private static String getLogOutputMode() {
        if (GlobalVars.globalSettings == null || GlobalVars.globalSettings.logOutputMode == null) {
            return GlobalSettings.LOG_OUTPUT_FILE;
        }
        return GlobalVars.globalSettings.logOutputMode;
    }

    private static boolean shouldLog(String level) {
        String logLevel = getLogLevel();
        if (GlobalSettings.LOG_LEVEL_DEBUG.equals(logLevel)) {
            return "信息".equals(level) || "调试".equals(level);
        }
        if (GlobalSettings.LOG_LEVEL_INFO.equals(logLevel)) {
            return "信息".equals(level);
        }
        return false;
    }

    public static void execute(String level, String msg) {
        String formatted = simpleDateFormat.format(new Date()) + " " + level.toUpperCase() + " -> " + msg;
        Handlers.log.post(() -> {
            if ("错误".equals(level)) {
                StatusBinderHub.signalError();
                fileLog(formatted);
                xposedLog(formatted);
                return;
            }

            if ("警告".equals(level)) {
                fileLog(formatted);
                xposedLog(formatted);
                return;
            }

            if (!shouldLog(level)) {
                return;
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

}
