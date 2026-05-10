package nep.timeline.cirno.hooks.android.input;

import android.os.Build;
import android.view.inputmethod.InputMethodInfo;

import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;
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
import nep.timeline.cirno.utils.ReflectUtils;

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
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.VANILLA_ICE_CREAM)
            return ReflectUtils.findParameterTypesOrDefault(
                XposedHelpers.findClassIfExists(getTargetClass(), classLoader),
                getTargetMethod(), String.class, int.class, int.class, int.class);
        return ReflectUtils.findParameterTypesOrDefault(
            XposedHelpers.findClassIfExists(getTargetClass(), classLoader),
            getTargetMethod(), String.class, int.class);
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

                    int userId = (Build.VERSION.SDK_INT > Build.VERSION_CODES.VANILLA_ICE_CREAM && param.args.length > 3)
                            ? (int) param.args[3]
                            : ActivityManagerService.getCurrentOrTargetUserId();
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
    public boolean isIgnoreError() {
        return true;
    }
}
