package nep.timeline.cirno.provide;

import nep.timeline.cirno.IApplicationInterface;
import nep.timeline.cirno.binder.BinderService;
import nep.timeline.cirno.log.Log;

public class ApplicationBinder {
    public static IApplicationInterface getInstance() {
        IApplicationInterface binder = BinderService.getApplicationBinder();
        if (binder == null) {
            Log.w("ApplicationBinder: binder missing");
        }
        return binder;
    }
}
