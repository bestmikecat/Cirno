package nep.timeline.cirno.services;

import java.lang.reflect.Array;

import de.robv.android.xposed.XposedHelpers;
import nep.timeline.cirno.entity.AppRecord;
import nep.timeline.cirno.log.Log;

public class NetworkManagementService {
    public static volatile Object instance;
    private static Object mNetdService;
    private static Class<?> UidRangeParcel;
    private static boolean isInitialized = false;  // 🔧 标记是否初始化成功

    public static void setInstance(Object obj, ClassLoader classLoader) {
        try {
            instance = obj;
            mNetdService = XposedHelpers.getObjectField(obj, "mNetdService");
            UidRangeParcel = XposedHelpers.findClass("android.net.UidRangeParcel", classLoader);
            
            // 🔧 检查初始化是否成功
            if (mNetdService != null && UidRangeParcel != null) {
                isInitialized = true;
                Log.d("NetworkManagementService 初始化成功");
            } else {
                isInitialized = false;
                Log.w("NetworkManagementService 初始化失败: mNetdService=" + (mNetdService != null) + ", UidRangeParcel=" + (UidRangeParcel != null));
            }
        } catch (Exception e) {
            isInitialized = false;
            Log.e("NetworkManagementService setInstance 异常", e);
        }
    }

    public static void socketDestroy(AppRecord appRecord) {
        // 🔧 检查初始化状态
        if (!isInitialized) {
            Log.w("socketDestroy: NetworkManagementService 未初始化，跳过");
            return;
        }

        // 🔧 检查参数有效性
        if (appRecord == null) {
            Log.w("socketDestroy: appRecord 为 null");
            return;
        }

        try {
            // 🔧 双重检查
            if (mNetdService == null || UidRangeParcel == null) {
                Log.w("socketDestroy: 服务未就绪");
                return;
            }

            Object uidRangeParcels = Array.newInstance(UidRangeParcel, 1);
            int uid = appRecord.getUid();
            
            // 🔧 尝试创建 UidRangeParcel 实例
            Object uidRange = XposedHelpers.newInstance(UidRangeParcel, uid, uid);
            Array.set(uidRangeParcels, 0, uidRange);
            
            // 🔧 尝试调用 socketDestroy，捕获所有可能的异常
            XposedHelpers.callMethod(mNetdService, "socketDestroy", uidRangeParcels, new int[0]);
            Log.d(appRecord.getPackageNameWithUser() + " 断开网络连接");
            
        } catch (NoSuchMethodError e) {
            // 🔧 方法不存在，说明该 Android 版本不支持
            Log.w("socketDestroy 方法不存在（该 Android 版本不支持）");
            isInitialized = false;  // 标记为未初始化，后续不再尝试
        } catch (UnsupportedOperationException e) {
            // 🔧 方法存在但不支持该操作
            Log.w("socketDestroy 操作不支持");
        } catch (Exception e) {
            // 🔧 其他异常（参数类型不匹配、空指针等）
            Log.e("socketDestroy 执行异常: " + e.getClass().getSimpleName(), e);
        }
    }
}