package nep.timeline.cirno.services;

import java.lang.reflect.Array;
import java.util.*;

import android.os.Build;

import de.robv.android.xposed.XposedHelpers;
import nep.timeline.cirno.entity.AppRecord;
import nep.timeline.cirno.log.Log;


public class NetworkManagementService {
    public static volatile Object instance;
    private static Object mNetdService;
    private static Class<?> UidRangeParcel;

    public static void setInstance(Object obj, ClassLoader classLoader) {
        instance = obj;
        if (Build.VERSION.SDK_INT >= 36) return;
        mNetdService = XposedHelpers.getObjectField(obj, "mNetdService");
        UidRangeParcel = XposedHelpers.findClass("android.net.UidRangeParcel", classLoader);
    }

    public static void socketDestroy(AppRecord appRecord) {
        int uid = appRecord.getUid();
        if (Build.VERSION.SDK_INT >= 36) {
            Set<Integer> uidSet = new HashSet<>();
            uidSet.add(uid);
            XposedHelpers.callMethod(instance, "destroyLiveTcpSocketsByOwnerUids", uidSet);
            Log.d(appRecord.getPackageNameWithUser() + " 断开网络连接");
        } else {
            Array.set(uidRangeParcels, 0, XposedHelpers.newInstance(UidRangeParcel, uid, uid));
            Object uidRangeParcels = Array.newInstance(UidRangeParcel, 1);
            XposedHelpers.callMethod(mNetdService, "socketDestroy", uidRangeParcels, new int[0]);
            Log.d(appRecord.getPackageNameWithUser() + " 断开网络连接");

        }
    }
}
