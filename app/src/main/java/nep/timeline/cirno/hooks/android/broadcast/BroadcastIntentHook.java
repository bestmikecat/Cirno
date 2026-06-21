package nep.timeline.cirno.hooks.android.broadcast;

import android.content.Intent;
import android.os.Build;

import java.lang.reflect.Method;

import nep.timeline.cirno.reflect.CakeHooker;
import nep.timeline.cirno.reflect.CakeReflection;
import nep.timeline.cirno.GlobalVars;
import nep.timeline.cirno.entity.AppRecord;
import nep.timeline.cirno.log.Log;
import nep.timeline.cirno.services.AppService;
import nep.timeline.cirno.services.FreezerService;
import nep.timeline.cirno.services.MonitorBinderHub;

public class BroadcastIntentHook {
    private static final String ACTION_TILE_CLICK = "nep.timeline.cirno.TILE_CLICK";

    public BroadcastIntentHook(ClassLoader classLoader) {
        try {
            Class<?> clazz = CakeReflection.findClassIfExists("com.android.server.am.ActivityManagerService", classLoader);

            if (clazz == null) {
                Log.e("无法监听广播意图，未找到 ActivityManagerService 类");
                return;
            }

            Method targetMethod = null;
            for (Method method : clazz.getDeclaredMethods())
                if (method.getName().equals("broadcastIntentLocked") && (targetMethod == null || targetMethod.getParameterTypes().length > method.getParameterTypes().length))
                    targetMethod = method;

            if (targetMethod == null) {
                Log.e("无法监听广播意图，未找到 broadcastIntentLocked 方法");
                return;
            }

            CakeHooker.hook(targetMethod, new CakeHooker.Callback() {
                @Override
                public void call(CakeHooker.BeforeHookCallback callback) {
                    int intentArgsIndex = 3;

                    int userIdIndex = 19;
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S_V2)
                        userIdIndex = 20;

                    Intent intent = (Intent) callback.getArgs()[intentArgsIndex];
                    int userId = (int) callback.getArgs()[userIdIndex];
                    if (intent != null) {
                        String action = intent.getAction();

                        if (ACTION_TILE_CLICK.equals(action)) {
                            String packageName = intent.getStringExtra("package_name");
                            if (packageName != null) {
                                FreezerService.temporaryUnfreezeIfNeed(packageName, userId, "控制中心磁贴", 3000);
                            }
                            return;
                        }

                        if (GlobalVars.ACTION_BINDER_REQUEST.equals(action)) {
                            Log.i("BroadcastIntentHook: binder request received, token=" + intent.getStringExtra(GlobalVars.EXTRA_BINDER_TOKEN));
                            MonitorBinderHub.publish("Binder request broadcast", intent.getStringExtra(GlobalVars.EXTRA_BINDER_TOKEN));
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
                        appRecord.setWaitingNotification(true);

                        FreezerService.temporaryUnfreezeIfNeed(appRecord, "MESSAGE PUSH", 1000L * GlobalVars.globalSettings.wakeFreezeDelay);
                    }
                }
            });

            Log.i("监听广播意图");
        } catch (Throwable throwable) {
            Log.e("监听广播意图失败", throwable);
        }
    }
}
