package nep.timeline.cirno.hooks.android.audio;

import android.media.AudioPlaybackConfiguration;
import android.os.Build;

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
        // ✅ 返回空数组，让 XposedBridge 自动适配所有参数组合
        return new Object[0];
    }

    @Override
    public XC_MethodHook getTargetHook() {
        return new AbstractMethodHook() {
            @Override
            protected void afterMethod(MethodHookParam param) {
                try {
                    // ✅ 安全地获取返回值
                    Object result = param.getResult();
                    if (result instanceof Boolean && !(boolean) result) {
                        return;
                    }

                    // ✅ 安全地获取第一个参数（事件）
                    if (param.args.length < 1) {
                        Log.w("AudioStateHook: 参数不足");
                        return;
                    }

                    Object arg0 = param.args[0];
                    if (!(arg0 instanceof Integer)) {
                        Log.w("AudioStateHook: 第一个参数不是 Integer");
                        return;
                    }

                    int event = (int) arg0;
                    Log.d("AudioStateHook 被调用: event=" + event + ", 参数个数=" + param.args.length);

                    if (!AudioHandler.LISTEN_EVENT.contains(event)) {
                        Log.d("AudioStateHook: 事件 " + event + " 不在监听列表中");
                        return;
                    }

                    // ✅ 获取 AudioPlaybackConfiguration 对象
                    AudioPlaybackConfiguration config = (AudioPlaybackConfiguration) param.thisObject;
                    if (config == null) {
                        Log.w("AudioStateHook: config 为 null");
                        return;
                    }

                    AudioPlaybackConfigurationReflect reflect = new AudioPlaybackConfigurationReflect(config);

                    Handlers.audio.post(() -> {
                        try {
                            int uid = reflect.getClientUid();
                            List<AppRecord> appRecords = AppService.getByUid(uid);
                            
                            Log.d("AudioStateHook: uid=" + uid + ", appRecords=" + 
                                  (appRecords == null ? "null" : appRecords.size()));
                            
                            if (appRecords == null) {
                                return;
                            }

                            int interfaceId = reflect.getPlayerInterfaceId();
                            
                            for (AppRecord appRecord : appRecords) {
                                if (appRecord == null) {
                                    continue;
                                }

                                Log.i("★ AudioStateHook 触发: " + appRecord.getPackageNameWithUser() + 
                                      ", event=" + event + ", interfaceId=" + interfaceId);
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
            // ✅ 修改 Hook 方式：使用反射找到所有 handleStateEvent 方法并 Hook
            Class<?> targetClass = XposedHelpers.findClass(getTargetClass(), classLoader);
            
            // 获取所有 handleStateEvent 方法
            boolean hooked = false;
            for (Method method : targetClass.getDeclaredMethods()) {
                if (method.getName().equals(getTargetMethod())) {
                    try {
                        XposedBridge.hookMethod(method, getTargetHook());
                        Log.i(method.getName() + " [参数: " + method.getParameterCount() + "] -> 成功Hook完毕!");
                        hooked = true;
     public XC_MethodHook getTargetHook() {
        return new AbstractMethodHook() {
            @Override
            protected void afterMethod(MethodHookParam param) {
                if (!((boolean) param.getResult()))
                    return;

                int event = (int) param.args[0];
                if (AudioHandler.LISTEN_EVENT.contains(event)) {
                    AudioPlaybackConfigurationReflect reflect = new AudioPlaybackConfigurationReflect((AudioPlaybackConfiguration) param.thisObject);

                    Handlers.audio.post(() -> {
                        List<AppRecord> appRecords = AppService.getByUid(reflect.getClientUid());
                        if (appRecords == null)
                            return;

                        int interfaceId = reflect.getPlayerInterfaceId();
                        for (AppRecord appRecord : appRecords) {
                            if (appRecord == null)
                                continue;

                            AudioHandler.call(appRecord, event, interfaceId);
                        }
                    });
                }
            }
        };
    }
}
