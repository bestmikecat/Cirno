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
    private static boolean socketDestroySupported = false;  // 🔧 一旦失败就永远不尝试
    private static boolean hasTestedSupport = false;  // 🔧 只测试一次

    public static void setInstance(Object obj, ClassLoader classLoader) {
        try {
            instance = obj;
            mNetdService = XposedHelpers.getObjectField(obj, "mNetdService");
            UidRangeParcel = XposedHelpers.findClass("android.net.UidRangeParcel", classLoader);
            
            if (mNetdService != null && UidRangeParcel != null) {
                Log.d("NetworkManagementService 初始化成功");
            } else {
                Log.w("NetworkManagementService 初始化失败: 缺少必要的类或字段");
                mNetdService = null;
                UidRangeParcel = null;
            }
        } catch (Exception e) {
            Log.e("NetworkManagementService setInstance 异常", e);
            mNetdService = null;
            UidRangeParcel = null;
        }
    }

    public static void socketDestroy(AppRecord appRecord) {
        // 🔧 已经测试过且不支持，直接返回
        if (hasTestedSupport && !socketDestroySupported) {
            return;
        }

        // 🔧 基本检查
        if (appRecord == null || mNetdService == null || UidRangeParcel == null) {
            return;
        }

        try {
            int uid = appRecord.getUid();
            
            // 🔧 构造参数
            Object uidRangeParcels = Array.newInstance(UidRangeParcel, 1);
            Object uidRange = XposedHelpers.newInstance(UidRangeParcel, uid, uid);
            Array.set(uidRangeParcels, 0, uidRange);
            
            // 🔧 尝试调用
            XposedHelpers.callMethod(mNetdService, "socketDestroy", uidRangeParcels, new int[0]);
            
            // 🔧 成功！标记支持
            if (!hasTestedSupport) {
                hasTestedSupport = true;
                socketDestroySupported = true;
                Log.i("✅ socketDestroy 功能可用");
            }
            Log.d(appRecord.getPackageNameWithUser() + " 断开网络连接");
            
        } catch (UnsupportedOperationException e) {
            // 🔧 首次失败：该ROM不支持socketDestroy
            if (!hasTestedSupport) {
                hasTestedSupport = true;
                socketDestroySupported = false;
                Log.w("❌ socketDestroy 功能不可用 (当前设备/ROM不支持此操作)");
            }
        } catch (Exception e) {
            // 🔧 其他异常也认为不支持
            if (!hasTestedSupport) {
                hasTestedSupport = true;
                socketDestroySupported = false;
                Log.w("❌ socketDestroy 功能不可用: " + e.getClass().getSimpleName());
            }
        }
    }
}