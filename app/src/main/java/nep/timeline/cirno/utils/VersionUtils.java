package nep.timeline.cirno.utils;

import android.os.Build;

public class VersionUtils {
    public static String getAndroidVersion() {
        return Build.VERSION.RELEASE == null ? "Unknown" : Build.VERSION.RELEASE;
    }
}
