package nep.timeline.cirno.hooks.android.broadcast;

import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Build;

import java.util.ArrayList;
import java.util.List;

import nep.timeline.cirno.configs.checkers.AppConfigs;
import nep.timeline.cirno.framework.MethodHook;
import nep.timeline.cirno.log.Log;
import nep.timeline.cirno.reflect.CakeHooker;
import nep.timeline.cirno.reflect.CakeReflection;
import nep.timeline.cirno.utils.ReflectUtils;

public class AutostartBlockHook extends MethodHook {
    public AutostartBlockHook(ClassLoader classLoader) {
        super(classLoader);
    }

    @Override
    public String getTargetClass() {
        return "com.android.server.pm.PackageManagerService";
    }

    @Override
    public String getTargetMethod() {
        return "queryIntentReceivers";
    }

    @Override
    public Object[] getTargetParam() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            return ReflectUtils.findParameterTypesOrDefault(
                    CakeReflection.findClassIfExists(getTargetClass(), classLoader),
                    getTargetMethod(), "com.android.server.pm.Computer", Intent.class, String.class, long.class, int.class);
        return ReflectUtils.findParameterTypesOrDefault(
                CakeReflection.findClassIfExists(getTargetClass(), classLoader),
                getTargetMethod(), Intent.class, String.class, long.class, int.class);
    }

    @Override
    public CakeHooker.Callback getTargetHook() {
        return new CakeHooker.Callback() {
            @Override
            public void call(CakeHooker.AfterHookCallback callback) {
                try {
                    if (callback.result == null) return;

                    Log.d("AutostartBlockHook: queryIntentReceivers called");

                    Object[] args = callback.getArgs();
                    int userId = (int) args[args.length - 1];

                    List<?> list;
                    try {
                        list = (List<?>) CakeReflection.callMethod(callback.result, "getList");
                    } catch (Throwable e) {
                        Log.w("AutostartBlockHook: getList failed", e);
                        return;
                    }

                    if (list == null || list.isEmpty()) return;

                    Log.d("AutostartBlockHook: getList size=" + list.size());

                    List<ResolveInfo> filtered = null;
                    for (int i = 0; i < list.size(); i++) {
                        ResolveInfo info = (ResolveInfo) list.get(i);
                        String pkg = (info.activityInfo != null) ? info.activityInfo.packageName : null;
                        if (pkg != null && AppConfigs.isAutostartBlocked(pkg, userId)) {
                            Log.i("AutostartBlockHook: blocked pkg=" + pkg + " userId=" + userId);
                            if (filtered == null) {
                                filtered = new ArrayList<>(list.size());
                                for (int j = 0; j < i; j++) {
                                    filtered.add((ResolveInfo) list.get(j));
                                }
                            }
                        } else if (filtered != null) {
                            filtered.add(info);
                        }
                    }

                    if (filtered != null) {
                        Log.d("AutostartBlockHook: filtered list size=" + filtered.size());
                        Object newList = CakeReflection.newInstance(callback.result.getClass(), new Class<?>[]{List.class}, filtered);
                        if (newList != null) {
                            callback.result = newList;
                        } else {
                            Log.w("AutostartBlockHook: newInstance failed");
                        }
                    }
                } catch (Throwable e) {
                    Log.e("AutostartBlockHook: error", e);
                }
            }
        };
    }
}
