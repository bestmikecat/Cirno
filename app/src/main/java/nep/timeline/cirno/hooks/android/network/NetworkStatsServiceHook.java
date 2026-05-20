package nep.timeline.cirno.hooks.android.network;

import android.os.Build;

import de.robv.android.xposed.XC_MethodHook;
import nep.timeline.cirno.framework.AbstractMethodHook;
import nep.timeline.cirno.framework.MethodHook;
import nep.timeline.cirno.services.NetworkSpeedMonitor;

public class NetworkStatsServiceHook extends MethodHook {
    public NetworkStatsServiceHook(ClassLoader classLoader) {
        super(classLoader);
    }

    @Override
    public String getTargetClass() {
        return "com.android.server.net.NetworkStatsService";
    }

    @Override
    public String getTargetMethod() {
        return null;
    }

    @Override
    public Object[] getTargetParam() {
        return new Object[]{
            android.content.Context.class,
            "android.net.INetd",
            android.app.AlarmManager.class,
            android.os.PowerManager.WakeLock.class,
            "com.android.server.net.NetworkStatsService$Clock",
            "com.android.server.net.NetworkStatsSettings",
            "com.android.server.net.NetworkStatsFactory",
            "com.android.server.net.NetworkStatsService$NetworkStatsObservers",
            "com.android.server.net.NetworkStatsService$Dependencies"
        };
    }

    @Override
    public XC_MethodHook getTargetHook() {
        return new AbstractMethodHook() {
            @Override
            protected void afterMethod(XC_MethodHook.MethodHookParam param) {
                NetworkSpeedMonitor.setInstance(param.thisObject, classLoader);
            }
        };
    }
}
