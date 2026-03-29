package nep.timeline.cirno.services;

import android.os.Build;
import java.lang.reflect.Array;

import de.robv.android.xposed.XposedHelpers;
import nep.timeline.cirno.entity.AppRecord;
import nep.timeline.cirno.log.Log;

public class NetworkManagementService {
    public static volatile Object instance;
    private static Object mNetdService;
    private static Class<?> UidRangeParcel;
    private static boolean isInitialized = false;
    private static int socketDestroyVersion = -1;  // 🔧 记录socketDestroy的版本支持情况

    public static void setInstance(Object obj, ClassLoader classLoader) {
        try {
            instance = obj;
            mNetdService = XposedHelpers.getObjectField(obj, "mNetdService");
            UidRangeParcel = XposedHelpers.findClass("android.net.UidRangeParcel", classLoader);
            
            if (mNetdService != null && UidRangeParcel != null) {
                isInitialized = true;
                detectSocketDestroySupport();
                Log.d("NetworkManagementService 初始化成功, socketDestroy版本: " + socketDestroyVersion);
            } else {
                isInitialized = false;
                Log.w("NetworkManagementService 初始化失败");
            }
        } catch (Exception e) {
            isInitialized = false;
            Log.e("NetworkManagementService setInstance 异常", e);
        }
    }

    // 🔧 检测该设备支持哪个版本的socketDestroy
    private static void detectSocketDestroySupport() {
        try {
            // Android 13+ 使用新的签名
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                socketDestroyVersion = 2;
            }
            // Android 12 及以下使用旧的签名
            else {
                socketDestroyVersion = 1;
            }
        } catch (Exception ignored) {
            socketDestroyVersion = 1;  // 默认尝试旧版本
        }
    }

    public static void socketDestroy(AppRecord appRecord) {
        if (!isInitialized || appRecord == null) {
            return;
        }

        try {
            if (mNetdService == null || UidRangeParcel == null) {
                return;
            }

            int uid = appRecord.getUid();
            
            try {
                if (socketDestroyVersion == 2) {
                    // 🔧 Android 13+ 的方式：传递 UidRangeParcel[]
                    Object uidRangeParcels = Array.newInstance(UidRangeParcel, 1);
                    Object uidRange = XposedHelpers.newInstance(UidRangeParcel, uid, uid);
                    Array.set(uidRangeParcels, 0, uidRange);
                    XposedHelpers.callMethod(mNetdService, "socketDestroy", uidRangeParcels, new int[0]);
                } else {
                    // 🔧 Android 12 及以下的方式：可能不支持或使用不同的参数
                    // 先尝试新版本，如果失败就标记为不支持
                    Object uidRangeParcels = Array.newInstance(UidRangeParcel, 1);
                    Object uidRange = XposedHelpers.newInstance(UidRangeParcel, uid, uid);
                    Array.set(uidRangeParcels, 0, uidRange);
                    XposedHelpers.callMethod(mNetdService, "socketDestroy", uidRangeParcels, new int[0]);
                }
                Log.d(appRecord.getPackageNameWithUser() + " 断开网络连接");
            } catch (UnsupportedOperationException e) {
                // 🔧 该Android版本的netd服务不支持socketDestroy操作
                // 这是正常的，某些定制ROM禁用了该功能
                socketDestroyVersion = 0;  // 标记为不支持
                Log.w("当前设备不支持socketDestroy操作（可能是ROM限制）");
                isInitialized = false;  // 后续不再尝试
            } catch (NoSuchMethodError e) {
                // 🔧 该Android版本没有socketDestroy方法
                socketDestroyVersion = 0;
                Log.w("当前Android版本不支持socketDestroy方法");
                isInitialized = false;
            }
        } catch (Throwable e) {
            // 🔧 其他未预期的异常
            Log.e("socketDestroy 执行失败", e);
        }
    }
}