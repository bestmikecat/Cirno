package nep.timeline.cirno.utils;

import android.os.Handler;

import com.topjohnwu.superuser.Shell;

import nep.timeline.cirno.threads.Handlers;

public class EnvUtils {
    public static Handler makeHandler(String name) {
        return Handlers.makeHandler(name);
    }

    public static boolean checkRoot() {
        return Shell.getShell().isRoot();
    }
}
