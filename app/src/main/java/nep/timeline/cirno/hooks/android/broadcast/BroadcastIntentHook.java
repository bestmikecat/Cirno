package nep.timeline.cirno.hooks.android.broadcast;

import android.content.Intent;
import android.os.Build;

import java.lang.reflect.Method;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import nep.timeline.cirno.GlobalVars;
import nep.timeline.cirno.entity.AppRecord;
import nep.timeline.cirno.framework.AbstractMethodHook;
import nep.timeline.cirno.log.Log;
import nep.timeline.cirno.services.AppService;
import nep.timeline.cirno.services.FreezerService;

public class BroadcastIntentHook {
    public BroadcastIntentHook(ClassLoader classLoader) {
        try {
            Class<?> clazz = XposedHelpers.findClassIfExists("com.android.server.am.ActivityManagerService", classLoader);
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                Class<?> controller = XposedHelpers.findClassIfExists("com.android.server.am.BroadcastController", classLoader);
                if (controller != null)
                    clazz = controller;
            }

            if (clazz == null) {
                Log.e("无法监听广播意图!");
                return;
            }

            Method targetMethod = null;
            for (Method method : clazz.getDeclaredMethods())
                if (method.getName().equals("broadcastIntentLocked") && (targetMethod == null || targetMethod.getParameterTypes().length < method.getParameterTypes().length))
                    targetMethod = method;

            if (targetMethod == null) {
                Log.e("无法监听广播意图!");
                return;
            }

            XposedBridge.hookMethod(targetMethod, new AbstractMethodHook() {
                @Override
                protected void beforeMethod(MethodHookParam param) {
                    int intentArgsIndex = 3;

                    int userIdIndex = 19;
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S_V2 && Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                        userIdIndex = 20;
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU)
                        userIdIndex = 21;

                    Intent intent = (Intent) param.args[intentArgsIndex];
                    int userId = (int) param.args[userIdIndex];
                    if (intent != null) {
                        String action = intent.getAction();

                        if (action == null || !action.endsWith(".android.c2dm.intent.RECEIVE") || action.equals("org.unifiedpush.android.connector.MESSAGE") || action.equals("com.meizu.flyme.push.intent.MESSAGE"))
                            return;

                        String packageName = (intent.getComponent() == null ? intent.getPackage() : intent.getComponent().getPackageName());

                        if (packageName == null)
                            return;

                        AppRecord appRecord = AppService.get(packageName, userId);
                        if (appRecord == null)
                            return;
                        Log.d(packageName + " 等待消息通知");
                        appRecord.setWaitingNotification(true);

                        FreezerService.temporaryUnfreezeIfNeed(appRecord, "MESSAGE PUSH", 1000L * GlobalVars.globalSettings.wakeFreezeDelay);
                    }
                }
            });

            Log.i("监听广播意图");
        } catch (Throwable throwable) {
            Log.e("无法监听广播意图, 异常:", throwable);
            Log.e("监听广播意图失败", throwable);
        }
    }
}