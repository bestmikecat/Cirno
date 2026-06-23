package nep.timeline.cirno.hooks.android.broadcast;

import android.content.Intent;
import android.os.Build;

import nep.timeline.cirno.GlobalVars;
import nep.timeline.cirno.configs.checkers.AppConfigs;
import nep.timeline.cirno.reflect.CakeHooker;
import nep.timeline.cirno.reflect.CakeReflection;
import nep.timeline.cirno.framework.MethodHook;
import nep.timeline.cirno.log.Log;
import nep.timeline.cirno.services.ProcessService;
import nep.timeline.cirno.utils.ReflectUtils;
import nep.timeline.cirno.utils.SystemChecker;
import nep.timeline.cirno.virtuals.ProcessRecord;

public class BroadcastSkipHook extends MethodHook {
    public BroadcastSkipHook(ClassLoader classLoader) {
        super(classLoader);
    }

    @Override
    public String getTargetClass() {
        return "com.android.server.am.BroadcastSkipPolicy";
    }

    @Override
    public String getTargetMethod() {
        return "shouldSkipMessage";
    }

    @Override
    public Object[] getTargetParam() {
        if (SystemChecker.isVivo(classLoader))
            return ReflectUtils.findParameterTypesOrDefault(
                    CakeReflection.findClassIfExists(getTargetClass(), classLoader),
                    getTargetMethod(), "com.android.server.am.BroadcastRecord", "com.android.server.am.BroadcastFilter", boolean.class, int.class, "com.android.server.am.IVivoBroadcastQueueModern");
        if (Build.VERSION.SDK_INT >= 36)
            return ReflectUtils.findParameterTypesOrDefault(
                    CakeReflection.findClassIfExists(getTargetClass(), classLoader),
                    getTargetMethod(), "com.android.server.am.BroadcastRecord", "com.android.server.am.BroadcastFilter", boolean.class);
        return ReflectUtils.findParameterTypesOrDefault(
                CakeReflection.findClassIfExists(getTargetClass(), classLoader),
                getTargetMethod(), "com.android.server.am.BroadcastRecord", "com.android.server.am.BroadcastFilter");
    }

    @Override
    public CakeHooker.Callback getTargetHook() {
        return new CakeHooker.Callback() {
            @Override
            public void call(CakeHooker.AfterHookCallback callback) {
                try {
                    if (callback.result != null) {
                        return;
                    }

                    Object record = callback.getArgs()[0];
                    if (record != null) {
                        Intent intent = (Intent) CakeReflection.getObjectField(record, "intent");
                        if (intent != null && GlobalVars.ACTION_BINDER.equals(intent.getAction())) {
                            return;
                        }
                    }

                    Object filter = callback.getArgs()[1];
                    if (filter == null) {
                        return;
                    }

                    Object receiver = CakeReflection.getObjectField(filter, "receiverList");
                    if (receiver == null) {
                        return;
                    }

                    Object app = CakeReflection.getObjectField(receiver, "app");
                    if (app == null) {
                        return;
                    }

                    ProcessRecord processRecord = ProcessService.getProcessRecord(app);
                    if (processRecord == null) {
                        return;
                    }

                    String packageName = processRecord.getPackageName();
                    int userId = processRecord.getUserId();
                    if (AppConfigs.isAutostartBlocked(packageName, userId)) {
                        callback.result = "Skipping deliver [Cirno]: autostart blocked";
                        return;
                    }

                    if (processRecord.isFrozen()) {
                        callback.result = "Skipping deliver [Cirno]: frozen process";
                    }
                } catch (Exception e) {
                    Log.e("BroadcastSkipHook 处理失败", e);
                }
            }
        };
    }

    @Override
    public int getMinVersion() {
        return Build.VERSION_CODES.UPSIDE_DOWN_CAKE;
    }
}
