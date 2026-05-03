package nep.timeline.cirno.provide;

import android.os.IBinder;
import android.os.IInterface;

import nep.timeline.cirno.binder.BinderService;
import nep.timeline.cirno.binders.ApplicationInterface;

public class ApplicationBinder extends ApplicationInterface.Stub {
    public static ApplicationInterface getInstance() {
        IBinder binder = BinderService.getBinder("Application");
        if (binder == null) {
            return null;
        }
        IInterface localInterface = binder.queryLocalInterface(DESCRIPTOR);
        return !(localInterface instanceof ApplicationInterface) ? new Proxy(binder) : (ApplicationInterface) localInterface;
    }

    @Override
    public java.util.List<String> getRunningApplication() {
        throw new UnsupportedOperationException();
    }
}
