package nep.timeline.cirno.hooks.android.binder;

import java.util.Arrays;

import de.robv.android.xposed.XC_MethodHook;
import nep.timeline.cirno.framework.AbstractMethodHook;
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
    public XC_MethodHook getTargetHook() {
        return new AbstractMethodHook() {
            @Override
            protected void beforeMethod(MethodHookParam param) {
                Log.d("unfreezeForKernel params: " + Arrays.toString(param.args));

                if (BinderService.received) {
                    unhook();
                    return;
                }

                int type = (int) param.args[0];
                if (type != BINDER_SYNC_TYPE)
                    return;
                int target = (int) param.args[4];

                FreezerService.temporaryUnfreezeIfNeed(target, "Binder", TEMP_UNFREEZE_INTERVAL_MS);
            }
        };
    }

    @Override
    public boolean isIgnoreError() {
        return !SystemChecker.isOplus(classLoader);
    }
}
