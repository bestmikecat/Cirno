package nep.timeline.cirno.hooks.android.camera;

import nep.timeline.cirno.reflect.CakeHooker;
import nep.timeline.cirno.framework.MethodHook;
import nep.timeline.cirno.handlers.CameraHandler;
import nep.timeline.cirno.log.Log;
import nep.timeline.cirno.threads.Handlers;

public class CameraBinderDiedHook extends MethodHook {
    public CameraBinderDiedHook(ClassLoader classLoader) {
        super(classLoader);
    }

    @Override
    public String getTargetClass() {
        return "com.android.server.camera.CameraServiceProxy";
    }

    @Override
    public String getTargetMethod() {
        return "binderDied";
    }

    @Override
    public Object[] getTargetParam() {
        return new Object[]{};
    }

    @Override
    public CakeHooker.Callback getTargetHook() {
        return new CakeHooker.Callback() {
            @Override
            public void call(CakeHooker.AfterHookCallback callback) {
                Handlers.camera.post(() -> {
                    try {
                        CameraHandler.clearAll();
                    } catch (Exception e) {
                        Log.e("CameraBinderDiedHook 处理异常", e);
                    }
                });
            }
        };
    }
}
