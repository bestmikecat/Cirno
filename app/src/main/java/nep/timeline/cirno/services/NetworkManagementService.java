package nep.timeline.cirno.services;

import java.lang.reflect.Array;

import android.os.Build;

import de.robv.android.xposed.XposedHelpers;
import nep.timeline.cirno.entity.AppRecord;
import nep.timeline.cirno.log.Log;

public class NetworkManagementService {
    public static volatile Object instance; 
    public static volatile Object connectivityService; 
    private static Object mNetdService;
    private static Class<?> UidRangeParcel;
    public static void setInstance(Object obj, ClassLoader classLoader) {
        instance = obj;
        if (Build.VERSION.SDK_INT >= 36) {
            connectivityService = obj;
        } else {
                mNetdService = XposedHelpers.getObjectField(obj, "mNetdService");
                UidRangeParcel = XposedHelpers.findClass(
                    "android.net.UidRangeParcel",
                    classLoader
                );
        }
    }

    public static void socketDestroy(AppRecord appRecord) {
        int uid = appRecord.getUid();

        try {
            if (Build.VERSION.SDK_INT >= 36) {
                if (connectivityService == null) {
                    Log.e("connectivityService is null");
                    return;
                }
                XposedHelpers.callMethod(
                        connectivityService,
                        "destroyLiveTcpSocketsByOwnerUids",
                        new int[]{uid}
                );
                Log.d(appRecord.getPackageNameWithUser() + " 断开TCP连接(Android16+)");
            } else {
                if (mNetdService == null || UidRangeParcel == null) {
                    Log.e("mNetdService or UidRangeParcel null");
                    return;
                }
                Object uidRangeParcels = Array.newInstance(UidRangeParcel, 1);

                Array.set(
                        uidRangeParcels,
                        0,
                        XposedHelpers.newInstance(UidRangeParcel, uid, uid)
                );

                XposedHelpers.callMethod(
                        mNetdService,
                        "socketDestroy",
                        uidRangeParcels,
                        new int[0]
                );
                Log.d(appRecord.getPackageNameWithUser() + " 断开网络连接(legacy)");
            }
        } catch (Throwable t) {
            Log.e("socketDestroy error: " + t);
        }
    }
}