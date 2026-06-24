package nep.timeline.cirno.hooks.android.broadcast;

import android.content.Intent;
import android.content.pm.ResolveInfo;

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
        return "com.android.server.pm.ResolveIntentHelper";
    }

    @Override
    public String getTargetMethod() {
        return "queryIntentReceiversInternal";
    }

    @Override
    public Object[] getTargetParam() {
        return ReflectUtils.findParameterTypesOrDefault(
                CakeReflection.findClassIfExists(getTargetClass(), classLoader),
                getTargetMethod(), "com.android.server.pm.Computer", Intent.class, String.class, long.class, int.class, int.class);
    }

    @Override
    public CakeHooker.Callback getTargetHook() {
        return new CakeHooker.Callback() {
            @Override
            public void call(CakeHooker.AfterHookCallback callback) {
                try {
                    if (callback.result == null) return;

                    Object[] args = callback.getArgs();
                    int userId = (int) args[4];

                    List<?> list = (List<?>) callback.result;
                    if (list == null || list.isEmpty()) return;

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
                        callback.result = filtered;
                    }
                } catch (Throwable e) {
                    Log.e("AutostartBlockHook: error", e);
                }
            }
        };
    }
}
