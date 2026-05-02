package nep.timeline.cirno.utils;

import android.os.Handler;

import nep.timeline.cirno.threads.Handlers;

public class EnvUtils {
    public static Handler makeHandler(String name) {
        return Handlers.makeHandler(name);
    }
}
