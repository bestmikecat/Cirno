package nep.timeline.cirno.provide;

import nep.timeline.cirno.binder.BinderService;
import nep.timeline.cirno.log.Log;

public class FrozenStateBinder {
    public static FrozenStateBinderFacade getInstance() {
        FrozenStateBinderFacade binder = BinderService.getFrozenStateBinder();
        if (binder == null) {
            Log.w("FrozenStateBinder: binder missing");
        }
        return binder;
    }
}
