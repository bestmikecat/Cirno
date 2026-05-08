package nep.timeline.cirno.hooks.android.notification;

import java.lang.reflect.Method;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import nep.timeline.cirno.GlobalVars;
import nep.timeline.cirno.entity.AppRecord;
import nep.timeline.cirno.framework.AbstractMethodHook;
import nep.timeline.cirno.log.Log;
import nep.timeline.cirno.services.AppService;

public class NotificationHook {
    public NotificationHook(ClassLoader classLoader) {
        try {
            Class<?> clazz = XposedHelpers.findClassIfExists("com.android.server.notification.NotificationManagerService", classLoader);

            if (clazz == null) {
                Log.e("无法监听通知意图!");
                return;
            }

            Method targetMethod = null;
            for (Method method : clazz.getDeclaredMethods())
                if (method.getName().equals("enqueueNotificationInternal") && (targetMethod == null || targetMethod.getParameterTypes().length < method.getParameterTypes().length))
                    targetMethod = method;

            if (targetMethod == null) {
                Log.e("无法监听通知意图!");
                return;
            }

            XposedBridge.hookMethod(targetMethod, new AbstractMethodHook() {
                @Override
                protected void beforeMethod(MethodHookParam param) {
                    int userId = (int) param.args[7];
                    String packageName = param.args[0].toString();
                    //Notification notification = (Notification) param.args[6];
                    AppRecord appRecord = AppService.get(packageName, userId);
                    if (appRecord != null && appRecord.isWaitingNotification()) {
                        appRecord.setWaitingNotification(false);
                        Log.d(packageName + " 接收消息通知");
                    }
                    /*
                    long postTime = notification.when;
                    long currentTime = System.currentTimeMillis();
                    Bundle extras = notification.extras;
                    String title = extras.getString(Notification.EXTRA_TITLE);
                    CharSequence text = extras.getCharSequence(Notification.EXTRA_TEXT);
                    Log.i("通知来源: " + packageName);
                    Log.i("内容: [" + title + "] " + text);
                    Log.i("通知设定时间: " + postTime);
                    Log.i("当前拦截时间: " + currentTime);
                    */
                }
            });

            Log.i("监听通知意图");
        } catch (Throwable throwable) {
            XposedBridge.log(GlobalVars.TAG + " -> 无法通知广播意图, 异常:");
            XposedBridge.log(throwable);
            Log.e("监听通知意图失败", throwable);
        }
    }
}
