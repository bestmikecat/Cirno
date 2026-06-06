package nep.timeline.cirno.hooks.android.binder;

import java.util.Arrays;

import nep.timeline.cirno.reflect.CakeHooker;
import nep.timeline.cirno.framework.MethodHook;
import nep.timeline.cirno.log.Log;
import nep.timeline.cirno.services.BinderService;
import nep.timeline.cirno.services.FreezerService;
import nep.timeline.cirno.utils.SystemChecker;

public class HansKernelUnfreezeHook extends MethodHook {
    private static final int BINDER_SYNC_TYPE = 1;
    private static final long TEMP_UNFREEZE_INTERVAL_MS = 3000L;

    public HansKernelUnfreezeHook(ClassLoader classLoader) {
        super(classLoader);
    }

    @Override
    public String getTargetClass() {
        return "com.android.server.am.OplusHansManager";
    }

    @Override
    public String getTargetMethod() {
        return "unfreezeForKernel";
    }

    @Override
    public Object[] getTargetParam() {
        return new Object[]{int.class, int.class, int.class, int.class, int.class, String.class, int.class};
    }

    @Override
    public CakeHooker.Callback getTargetHook() {
        return new CakeHooker.Callback() {
            @Override
            public void call(CakeHooker.BeforeHookCallback callback) {
                Log.d("unfreezeForKernel params: " + Arrays.toString(callback.getArgs()));

                if (BinderService.received) {
                    unhook();
                    return;
                }

                int type = (int) callback.getArgs()[0];
                if (type != BINDER_SYNC_TYPE)
                    return;
                int target = (int) callback.getArgs()[4];

                FreezerService.temporaryUnfreezeIfNeed(target, "Binder", TEMP_UNFREEZE_INTERVAL_MS);
            }
        };
    }

    @Override
    public boolean isIgnoreError() {
        return !SystemChecker.isOplus(classLoader);
    }
}
