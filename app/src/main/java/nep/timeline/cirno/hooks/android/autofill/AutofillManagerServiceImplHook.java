package nep.timeline.cirno.hooks.android.autofill;

import android.content.pm.ServiceInfo;
import android.service.autofill.AutofillServiceInfo;

import nep.timeline.cirno.entity.AppRecord;
import nep.timeline.cirno.framework.MethodHook;
import nep.timeline.cirno.log.Log;
import nep.timeline.cirno.reflect.CakeHooker;
import nep.timeline.cirno.reflect.CakeReflection;
import nep.timeline.cirno.services.AppService;
import nep.timeline.cirno.services.FreezerService;
import nep.timeline.cirno.threads.FreezerHandler;
import nep.timeline.cirno.utils.AutofillData;
import nep.timeline.cirno.utils.ReflectUtils;

public class AutofillManagerServiceImplHook extends MethodHook {
    private static final long INVALID_SESSION_ID = 0L;
    private static final long FAILED_SESSION_ID = 0x7fffffffL;

    public AutofillManagerServiceImplHook(ClassLoader classLoader) {
        super(classLoader);
    }

    @Override
    public String getTargetClass() {
        return "com.android.server.autofill.AutofillManagerServiceImpl";
    }

    @Override
    public String getTargetMethod() {
        return "startSessionLocked";
    }

    @Override
    public Object[] getTargetParam() {
        return ReflectUtils.findParameterTypesOrDefault(
                CakeReflection.findClassIfExists(getTargetClass(), classLoader),
                getTargetMethod(),
                android.os.IBinder.class,
                int.class,
                int.class,
                android.os.IBinder.class,
                android.view.autofill.AutofillId.class,
                android.graphics.Rect.class,
                android.view.autofill.AutofillValue.class,
                boolean.class,
                android.content.ComponentName.class,
                boolean.class,
                boolean.class,
                int.class);
    }

    @Override
    public CakeHooker.Callback getTargetHook() {
        return new CakeHooker.Callback() {
            @Override
            public void call(CakeHooker.AfterHookCallback callback) {
                try {
                    AutofillData.instance = callback.getThisObject();

                    Object result = callback.getResult();
                    if (!(result instanceof Long sessionResult)) {
                        return;
                    }

                    if (sessionResult == INVALID_SESSION_ID || sessionResult == FAILED_SESSION_ID) {
                        return;
                    }

                    Object infoObject = CakeReflection.getObjectField(callback.getThisObject(), "mInfo");
                    if (!(infoObject instanceof AutofillServiceInfo autofillServiceInfo)) {
                        return;
                    }

                    ServiceInfo serviceInfo = autofillServiceInfo.getServiceInfo();
                    if (serviceInfo == null || serviceInfo.packageName == null || serviceInfo.packageName.isEmpty()) {
                        return;
                    }

                    int userId = CakeReflection.getIntField(callback.getThisObject(), "mUserId");
                    AppRecord appRecord = AppService.get(serviceInfo.packageName, userId);
                    if (appRecord == null || appRecord.equals(AutofillData.currentAutofillApp)) {
                        return;
                    }

                    AppRecord oldApp = AutofillData.currentAutofillApp;
                    AutofillData.currentAutofillApp = appRecord;
                    FreezerService.thaw(appRecord);
                    if (oldApp != null) {
                        FreezerHandler.sendFreezeMessage(oldApp);
                    }
                } catch (Throwable e) {
                    Log.e("AutofillManagerServiceImpl 处理失败", e);
                }
            }
        };
    }
}
