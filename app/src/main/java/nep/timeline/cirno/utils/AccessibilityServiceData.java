package nep.timeline.cirno.utils;

import android.content.ComponentName;
import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;

import nep.timeline.cirno.log.Log;
import nep.timeline.cirno.services.ActivityManagerService;

public class AccessibilityServiceData {
    public static boolean isEnabledAccessibilityApp(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return false;
        }

        String value = getEnabledAccessibilityServices();
        if (TextUtils.isEmpty(value)) {
            return false;
        }

        String[] services = value.split(":");
        for (String service : services) {
            ComponentName componentName = ComponentName.unflattenFromString(service);
            if (componentName != null && packageName.equals(componentName.getPackageName())) {
                return true;
            }
        }
        return false;
    }

    private static String getEnabledAccessibilityServices() {
        Context context;
        try {
            context = ActivityManagerService.getContext();
        } catch (Throwable e) {
            Log.e("获取 ActivityManagerService Context", e);
            return null;
        }
        if (context == null) {
            return null;
        }

        try {
            return Settings.Secure.getString(
                    context.getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            );
        } catch (Throwable e) {
            Log.e("获取无障碍服务", e);
            return null;
        }
    }
}
