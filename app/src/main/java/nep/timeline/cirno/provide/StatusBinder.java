package nep.timeline.cirno.provide;

import nep.timeline.cirno.binder.BinderService;
import nep.timeline.cirno.log.Log;

public class StatusBinder {
    public static StatusBinderFacade getInstance() {
        StatusBinderFacade binder = BinderService.getStatusBinder();
        if (binder == null) {
            Log.w("StatusBinder: binder missing");
        }
        return binder;
    }
}
