package nep.timeline.cirno.hooks.android.activity;

import de.robv.android.xposed.XC_MethodHook;
import nep.timeline.cirno.framework.AbstractMethodHook;
import nep.timeline.cirno.framework.MethodHook;
import nep.timeline.cirno.services.MonitorBinderHub;

public class ActivityManagerSystemReadyHook extends MethodHook {
    public ActivityManagerSystemReadyHook(ClassLoader classLoader) {
        super(classLoader);
    }

    @Override
    public String getTargetClass() {
        return "com.android.server.am.ActivityManagerService";
    }

    @Override
    public String getTargetMethod() {
        return "systemReady";
    }

    @Override
    public Object[] getTargetParam() {
        return new Object[]{Runnable.class, Object.class};
    }

    @Override
    public XC_MethodHook getTargetHook() {
        return new AbstractMethodHook() {
            @Override
            protected void afterMethod(MethodHookParam param) {
                MonitorBinderHub.setBootCompleted();
                MonitorBinderHub.publish("ActivityManagerService.systemReady");
            }
        };
    }
}
