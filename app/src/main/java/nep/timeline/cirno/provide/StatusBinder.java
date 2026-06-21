package nep.timeline.cirno.provide;

import nep.timeline.cirno.IStatusInterface;
import nep.timeline.cirno.binder.BinderService;
import nep.timeline.cirno.log.Log;

public class StatusBinder {
    public static IStatusInterface getInstance() {
        IStatusInterface binder = BinderService.getStatusBinder();
        if (binder == null) {
            Log.w("StatusBinder: binder missing");
        }
        return binder;
    }
}
