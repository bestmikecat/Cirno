package nep.timeline.cirno.utils;

import java.io.File;

import nep.timeline.cirno.reflect.CakeReflection;

public class SystemChecker {
    private static final String TOMB_STONE_ADD_ON_PROP = "/data/adb/modules/lib_tombstone/module.prop";

    public static boolean isSamsung(ClassLoader classLoader) {
        return CakeReflection.findClassIfExists("com.android.server.am.FreecessController", classLoader) != null;
    }

    public static boolean isXiaomi(ClassLoader classLoader) {
        return CakeReflection.findClassIfExists("com.miui.server.greeze.GreezeManagerService", classLoader) != null;
    }

    public static boolean isOplus(ClassLoader classLoader) {
        return CakeReflection.findClassIfExists("com.android.server.am.OplusHansManager", classLoader) != null;
    }

    public static boolean isHuawei(ClassLoader classLoader) {
        return CakeReflection.findClassIfExists("com.huawei.turbozone.ITurboService", classLoader) != null;
    }

    public static boolean isVivo(ClassLoader classLoader) {
        return CakeReflection.findClassIfExists("com.android.server.am.IVivoBroadcastQueueModern", classLoader) != null;
    }

    public static boolean isNubia(ClassLoader classLoader) {
        return CakeReflection.findClassIfExists("cn.nubia.server.appmgmt.ApplicationControllerUtils", classLoader) != null;
    }

    public static boolean isTombStoneAddOnEnabled() {
        return new File(TOMB_STONE_ADD_ON_PROP).exists();
    }
}
