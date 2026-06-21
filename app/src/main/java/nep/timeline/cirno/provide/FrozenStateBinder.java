package nep.timeline.cirno.provide;

import nep.timeline.cirno.IFrozenStateInterface;
import nep.timeline.cirno.binder.BinderService;
import nep.timeline.cirno.log.Log;

public class FrozenStateBinder {
    public static IFrozenStateInterface getInstance() {
        IFrozenStateInterface binder = BinderService.getFrozenStateBinder();
        if (binder == null) {
            Log.w("FrozenStateBinder: binder missing");
        }
        return binder;
    }
}
