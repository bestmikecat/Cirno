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
import nep.timeline.cirno.services.ConfigBinderHub;
import nep.timeline.cirno.services.FreezerService;

public class BroadcastIntentHook {
    private static final String ACTION_HOOK_READY = "nep.timeline.cirno.HOOK_READY";
    private static final String ACTION_TILE_CLICK = "nep.timeline.cirno.TILE_CLICK";

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

                        if (ACTION_HOOK_READY.equals(action)) {
                            String scope = intent.getStringExtra("scope");
                            if ("systemui".equals(scope)) {
                                ConfigBinderHub.setSignal(ConfigBinderHub.SIGNAL_SYSTEMUI_HOOK_READY, "1");
                                Log.i("SystemUI hook ready");
                            }
                            return;
                        }

                        if (ACTION_TILE_CLICK.equals(action)) {
                            String logLevel = intent.getStringExtra("log_level");
                            String logMsg = intent.getStringExtra("log_msg");
                            if (logMsg != null) {
                                if ("e".equals(logLevel)) {
                                    Log.e(logMsg);
                                } else if ("w".equals(logLevel)) {
                                    Log.w(logMsg);
                                } else if ("d".equals(logLevel)) {
                                    Log.d(logMsg);
                                } else {
                                    Log.i(logMsg);
                                }
                            }
                            String packageName = intent.getStringExtra("package_name");
                            if (packageName != null) {
                                FreezerService.temporaryUnfreezeIfNeed(packageName, userId, "控制中心磁贴", 3000);
                            }
                            return;
                        }

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
