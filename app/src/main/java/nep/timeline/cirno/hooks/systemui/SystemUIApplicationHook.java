package nep.timeline.cirno.hooks.systemui;

import android.content.Context;
import android.content.Intent;

import nep.timeline.cirno.reflect.CakeHooker;
import nep.timeline.cirno.framework.MethodHook;
import nep.timeline.cirno.log.Log;

public class SystemUIApplicationHook extends MethodHook {
    private static final String ACTION_HOOK_READY = "nep.timeline.cirno.HOOK_READY";

    public SystemUIApplicationHook(ClassLoader classLoader) {
        super(classLoader);
    }

    @Override
    public String getTargetClass() {
        return "com.android.systemui.SystemUIApplication";
    }

    @Override
    public String getTargetMethod() {
        return "onCreate";
    }

    @Override
    public Object[] getTargetParam() {
        return new Object[0];
    }

    @Override
    public CakeHooker.Callback getTargetHook() {
        return new CakeHooker.Callback() {
            @Override
            public void call(CakeHooker.AfterHookCallback callback) {
                try {
                    if (!(callback.getThisObject() instanceof Context)) {
                        Log.w("SystemUIApplication 不是 Context，无法上报 SystemUI hook 状态");
                        return;
                    }
                    Intent intent = new Intent(ACTION_HOOK_READY);
                    intent.putExtra("scope", "systemui");
                    intent.setPackage("android");
                    ((Context) callback.getThisObject()).sendBroadcast(intent);
                    Log.i("SystemUI hook ready");
                } catch (Throwable t) {
                    Log.e("上报 SystemUI hook 状态失败", t);
                } finally {
                    unhook();
                }
            }
        };
    }
}
