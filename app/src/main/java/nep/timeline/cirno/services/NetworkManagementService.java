package nep.timeline.cirno.services;

import java.lang.reflect.Array;
import java.util.HashSet;
import java.util.Set;

import de.robv.android.xposed.XposedHelpers;
import nep.timeline.cirno.entity.AppRecord;
import nep.timeline.cirno.log.Log;

public class NetworkManagementService {
    public static volatile ClassLoader instance;

    public static void setInstance(ClassLoader classLoader) {
        instance = classLoader;
    }

    public static void socketDestroy(AppRecord appRecord) {
        Set<Integer> uids = new HashSet<>();
        Class<?> inetDiagCls = XposedHelpers.findClassIfExists("com.android.net.module.util.netlink.InetDiagMessage",
                instance);
        int uid = appRecord.getUid();
        if (inetDiagCls != null) {
            uids.add(uid);
            XposedHelpers.callStaticMethod(inetDiagCls, "destroyLiveTcpSocketsByOwnerUids", uids);
        }
        Log.d(appRecord.getPackageNameWithUser() + " 断开网络连接");
    }
}