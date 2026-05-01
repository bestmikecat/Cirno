package nep.timeline.cirno.hooks.android.input;

import android.os.Build;
import android.view.inputmethod.InputMethodInfo;

import java.lang.reflect.Method;
import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import nep.timeline.cirno.entity.AppRecord;
import nep.timeline.cirno.framework.AbstractMethodHook;
import nep.timeline.cirno.framework.MethodHook;
import nep.timeline.cirno.log.Log;
import nep.timeline.cirno.services.ActivityManagerService;
import nep.timeline.cirno.services.AppService;
import nep.timeline.cirno.services.FreezerService;
import nep.timeline.cirno.threads.FreezerHandler;
import nep.timeline.cirno.utils.InputMethodData;

public class InputMethodManagerService extends MethodHook {
    @SuppressWarnings("unchecked")
    private Map<String, InputMethodInfo> resolveInputMethodMap(Object settings) {
        if (settings == null) {
            return null;
        }

        try {
            Object map = XposedHelpers.getObjectField(settings, "mMethodMap");
            if (map == null) {
                return null;
            }

            if ("com.android.server.inputmethod.InputMethodMap".equals(map.getClass().getTypeName())) {
                return (Map<String, InputMethodInfo>) XposedHelpers.getObjectField(map, "mMap");
            }

            return (Map<String, InputMethodInfo>) map;
        } catch (Exception e) {
            return null;
        }
    }

    private Object resolveInputMethodSettings(Object service, int userId) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                return XposedHelpers.callStaticMethod(
                        XposedHelpers.findClassIfExists(
                                "com.android.server.inputmethod.InputMethodSettingsRepository",
                                classLoader),
                        "get", userId);
            } catch (Exception e) {
                Log.w("获取 InputMethodSettingsRepository 失败");
                return null;
            }
        }

        try {
            return XposedHelpers.getObjectField(service, "mSettings");
        } catch (Exception e) {
            Log.w("获取 mSettings 失败");
            return null;
        }
    }

    public InputMethodManagerService(ClassLoader classLoader) {
        super(classLoader);
    }

    @Override
    public String getTargetClass() {
        return "com.android.server.inputmethod.InputMethodManagerService";
    }

    @Override
    public String getTargetMethod() {
        return "setInputMethodLocked";
    }

    @Override
    public Object[] getTargetParam() {
        // ✅ 返回空数组，让 Xposed 自动适配所有参数组合
        return new Object[0];
    }

    @Override
    public XC_MethodHook getTargetHook() {
        return new AbstractMethodHook() {
            private String getPackageNameFromId(String id) {
                if (id == null)
                    return null;
                int slash = id.indexOf('/');
                if (slash <= 0)
                    return id;
                return id.substring(0, slash);
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void beforeMethod(MethodHookParam param) {
                try {
                    if (param.args.length < 1) {
                        return;
                    }

                    Object arg0 = param.args[0];
                    if (!(arg0 instanceof String id)) {
                        return;
                    }

                    if (id == null || id.isEmpty()) {
                        return;
                    }

                    int userId = ActivityManagerService.getCurrentOrTargetUserId();
                    Object settings = resolveInputMethodSettings(param.thisObject, userId);

                    synchronized (InputMethodData.class) {
                        if (InputMethodData.instance == null) {
                            InputMethodData.instance = param.thisObject;
                        }

                        InputMethodData.inputMethods = resolveInputMethodMap(settings);

                        Map<String, InputMethodInfo> inputMethodMap = InputMethodData.inputMethods;
                        String pkgFromId = getPackageNameFromId(id);
                        InputMethodInfo inputMethodInfo = inputMethodMap == null ? null : inputMethodMap.get(id);
                        String pkgName = inputMethodInfo == null ? pkgFromId : inputMethodInfo.getPackageName();
                        if (pkgFromId != null && pkgName != null && !pkgFromId.equals(pkgName)) {
                            // id 是来源真值，避免输入法映射缓存导致包名错误
                            pkgName = pkgFromId;
                        }

                        InputMethodData.currentInputMethodInfo = inputMethodInfo;
                        AppRecord appRecord = AppService.get(pkgName, userId);
                        if (appRecord != InputMethodData.currentInputMethodApp) {
                            AppRecord oldApp = InputMethodData.currentInputMethodApp;
                            InputMethodData.currentInputMethodApp = appRecord;
                            if (appRecord != null) {
                                FreezerService.thaw(appRecord);
                            }
                            if (oldApp != null) {
                                FreezerHandler.sendFreezeMessage(oldApp);
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e("InputMethodManagerService 处理失败", e);
                }
            }
        };
    }

    @Override
    public void startHook() {
        try {
            Class<?> targetClass = XposedHelpers.findClass(getTargetClass(), classLoader);

            // ✅ 遍历所有 setInputMethodLocked 方法，不管参数是什么
            boolean hooked = false;
            for (Method method : targetClass.getDeclaredMethods()) {
                if (method.getName().equals(getTargetMethod())) {
                    try {
                        XposedBridge.hookMethod(method, getTargetHook());
                        Log.i(method.getName() + " [参数: " + method.getParameterCount() + "] -> 成功Hook完毕!");
                        hooked = true;
                    } catch (Exception e) {
                        Log.w("Hook " + method.getName() + " [参数: " + method.getParameterCount() + "] 失败");
                    }
                }
            }

            if (!hooked) {
                Log.w("未能 Hook 任何 setInputMethodLocked 方法");
            }
        } catch (Throwable e) {
            Log.e(getTargetMethod() + " Hook 失败", e);
        }
    }

    @Override
    public boolean isIgnoreError() {
        // ✅ 忽略错误，允许部分失败
        return true;
    }
}
