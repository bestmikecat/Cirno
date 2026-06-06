package nep.timeline.cirno.hooks.android.audio;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import nep.timeline.cirno.reflect.CakeHooker;
import nep.timeline.cirno.reflect.CakeReflection;
import nep.timeline.cirno.log.Log;
import nep.timeline.cirno.services.FreezerService;

public class SendMediaButtonHook {
    public SendMediaButtonHook(ClassLoader classLoader) {
        Class<?> targetClass = CakeReflection.findClassIfExists(
                "com.android.server.media.MediaSessionRecord$SessionCb", classLoader);

        if (targetClass == null) {
            Log.w("MediaSessionRecord$SessionCb 不存在");
            return;
        }

        String fieldName = null;

        for (Field field : targetClass.getDeclaredFields()) {
            if (field.getType().getName().equals("com.android.server.media.MediaSessionRecord")) {
                fieldName = field.getName();
                break;
            }
        }

        if (fieldName == null) {
            Log.w("无法监听媒体按键!");
            return;
        }

        List<Method> methods = new ArrayList<>();
        for (Method method : targetClass.getDeclaredMethods()) {
            String methodName = method.getName();
            if (methodName.equals("sendMediaButton") || methodName.equals("play") ||
                    methodName.equals("playFromMediaId") || methodName.equals("playFromSearch") ||
                    methodName.equals("playFromUri") || methodName.equals("next") ||
                    methodName.equals("previous") || methodName.equals("seekTo")) {
                methods.add(method);
            }
        }

        for (Method method : methods) {
            try {
                String finalFieldName = fieldName;
                CakeHooker.hook(method, new CakeHooker.Callback() {
                    @Override
                    public void call(CakeHooker.BeforeHookCallback callback) {
                        try {
                            Object record = CakeReflection.getObjectField(callback.getThisObject(), finalFieldName);
                            if (record == null) {
                                return;
                            }

                            int uid = CakeReflection.getIntField(record, "mOwnerUid");
                            FreezerService.temporaryUnfreezeIfNeed(uid, "媒体按键", 3000);
                        } catch (Exception e) {
                            Log.e("媒体按键处理失败", e);
                        }
                    }
                });
                Log.i(method.getName() + " -> 成功Hook完毕!");
            } catch (Throwable throwable) {
                Log.e(method.getName(), throwable);
            }
        }
    }
}
