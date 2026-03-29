package nep.timeline.cirno.threads;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;

import nep.timeline.cirno.GlobalVars;
import nep.timeline.cirno.log.Log;

public class Handlers {
    public static final Handler alarms = makeHandler("Alarms");
    public static final Handler network = makeHandler("Network");
    public static final Handler audio = makeHandler("Audio");
    public static final Handler location = makeHandler("Location");
    public static final Handler rekernel = makeHandler("ReKernel");
    public static final Handler log = makeHandlerBackground("Log");
    public static final Handler config = makeHandlerBackground("Config");

    public static Handler makeHandlerForeground(String str) {
        return makeHandlerForeground(str, false);
    }

    public static Handler makeHandlerForeground(String str, boolean async) {
        if (async)
            return Handler.createAsync(makeLooperForeground(str));
        else
            return new Handler(makeLooperForeground(str));
    }

    public static Handler makeHandler(String str) {
        return makeHandler(str, false);
    }

    public static Handler makeHandler(String str, boolean async) {
        if (async)
            return Handler.createAsync(makeLooper(str));
        else
            return new Handler(makeLooper(str));
    }

    public static Handler makeHandlerBackground(String str) {
        return makeHandlerBackground(str, false);
    }

    public static Handler makeHandlerBackground(String str, boolean async) {
        if (async)
            return Handler.createAsync(makeLooperBackground(str));
        else return new Handler(makeLooperBackground(str));
    }

    public static Looper makeLooperForeground(String str) {
        HandlerThread handlerThread = new HandlerThread(GlobalVars.TAG + "-" + str, Process.THREAD_PRIORITY_FOREGROUND);
        // 🔧 改进的异常处理：记录完整堆栈
        handlerThread.setUncaughtExceptionHandler((t, e) -> {
            Log.e("线程 " + t.getName() + " 出现异常: " + e.getClass().getSimpleName());
            if (e.getCause() != null) {
                Log.e("  根因: " + e.getCause().getClass().getSimpleName() + ": " + e.getCause().getMessage());
            }
            Log.e("完整堆栈:");
            for (StackTraceElement element : e.getStackTrace()) {
                Log.e("  at " + element.getClassName() + "." + element.getMethodName() + "(" + element.getFileName() + ":" + element.getLineNumber() + ")");
            }
        });
        handlerThread.start();
        return handlerThread.getLooper();
    }

    public static Looper makeLooperBackground(String str) {
        HandlerThread handlerThread = new HandlerThread(GlobalVars.TAG + "-" + str, Process.THREAD_PRIORITY_BACKGROUND);
        // 🔧 改进的异常处理：记录完整堆栈
        handlerThread.setUncaughtExceptionHandler((t, e) -> {
            Log.e("线程 " + t.getName() + " 出现异常: " + e.getClass().getSimpleName());
            if (e.getCause() != null) {
                Log.e("  根因: " + e.getCause().getClass().getSimpleName() + ": " + e.getCause().getMessage());
            }
            Log.e("完整堆栈:");
            for (StackTraceElement element : e.getStackTrace()) {
                Log.e("  at " + element.getClassName() + "." + element.getMethodName() + "(" + element.getFileName() + ":" + element.getLineNumber() + ")");
            }
        });
        handlerThread.start();
        return handlerThread.getLooper();
    }

    public static Looper makeLooper(String str) {
        HandlerThread handlerThread = new HandlerThread(GlobalVars.TAG + "-" + str);
        // 🔧 改进的异常处理：记录完整堆栈
        handlerThread.setUncaughtExceptionHandler((t, e) -> {
            Log.e("线程 " + t.getName() + " 出现异常: " + e.getClass().getSimpleName());
            if (e.getCause() != null) {
                Log.e("  根因: " + e.getCause().getClass().getSimpleName() + ": " + e.getCause().getMessage());
            }
            Log.e("完整堆栈:");
            for (StackTraceElement element : e.getStackTrace()) {
                Log.e("  at " + element.getClassName() + "." + element.getMethodName() + "(" + element.getFileName() + ":" + element.getLineNumber() + ")");
            }
        });
        handlerThread.start();
        return handlerThread.getLooper();
    }
}