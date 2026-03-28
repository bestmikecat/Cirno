package nep.timeline.cirno.hooks.android.audio;

import android.media.AudioPlaybackConfiguration;

import java.lang.reflect.Method;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import nep.timeline.cirno.entity.AppRecord;
import nep.timeline.cirno.framework.AbstractMethodHook;
import nep.timeline.cirno.framework.MethodHook;
import nep.timeline.cirno.handlers.AudioHandler;
import nep.timeline.cirno.log.Log;
import nep.timeline.cirno.services.AppService;
import nep.timeline.cirno.threads.Handlers;
import nep.timeline.cirno.virtuals.AudioPlaybackConfigurationReflect;

public class AudioStateHook extends MethodHook {
    public AudioStateHook(ClassLoader classLoader) {
        super(classLoader);
    }

    @Override
    public String getTargetClass() {
        return AudioPlaybackConfiguration.class.getTypeName();
    }

    @Override
    public String getTargetMethod() {
        return "handleStateEvent";
    }

    @Override
    public Object[] getTargetParam() {
        return new Object[0];
    }

    @Override
    public XC_MethodHook getTargetHook() {
        return new AbstractMethodHook() {
            @Override
            protected void afterMethod(MethodHookParam param) {
                try {
                    Object result = param.getResult();
                    if (result instanceof Boolean && !(boolean) result) {
                        return;
                    }

                    if (param.args.length < 1) {
                        return;
                    }

                    Object arg0 = param.args[0];
                    if (!(arg0 instanceof Integer)) {
                        return;
                    }

                    int event = (int) arg0;

                    if (!AudioHandler.LISTEN_EVENT.contains(event)) {
                        return;
                    }

                    AudioPlaybackConfiguration config = (AudioPlaybackConfiguration) param.thisObject;
                    if (config == null) {
                        return;
                    }

                    AudioPlaybackConfigurationReflect reflect = new AudioPlaybackConfigurationReflect(config);

                    Handlers.audio.post(() -> {
                        try {
                            int uid = reflect.getClientUid();
                            List<AppRecord> appRecords = AppService.getByUid(uid);
                            
                            if (appRecords == null) {
                                return;
                            }

                            int interfaceId = reflect.getPlayerInterfaceId();
                            
                            for (AppRecord appRecord : appRecords) {
                                if (appRecord == null) {
                                    continue;
                                }

                                AudioHandler.call(appRecord, event, interfaceId);
                            }
                        } catch (Exception e) {
                            Log.e("AudioStateHook 处理异常", e);
                        }
                    });
                } catch (Exception e) {
                    Log.e("AudioStateHook 异常", e);
                }
            }
        };
    }

    @Override
    public void startHook() {
        try {
            Class<?> targetClass = XposedHelpers.findClass(getTargetClass(), classLoader);
            
            boolean hooked = false;
            for (Method method : targetClass.getDeclaredMethods()) {
                if (method.getName().equals(getTargetMethod())) {
                    try {
                        XposedBridge.hookMethod(method, getTargetHook());
                        hooked = true;
                    } catch (Exception e) {
                        // 忽略失败
                    }
                }
            }
            
            if (hooked) {
                Log.i("handleStateEvent -> 成功Hook完毕!");
            }
        } catch (Throwable e) {
            Log.e(getTargetMethod() + " Hook 失败", e);
        }
    }

    @Override
    public boolean isIgnoreError() {
        return true;
    }
}