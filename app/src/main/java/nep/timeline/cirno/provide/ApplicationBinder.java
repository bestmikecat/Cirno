package nep.timeline.cirno.provide;

import android.os.IBinder;
import android.os.IInterface;

import nep.timeline.cirno.binder.BinderService;
import nep.timeline.cirno.binders.ApplicationInterface;
import nep.timeline.cirno.log.Log;

public class ApplicationBinder extends ApplicationInterface.Stub {
    public static ApplicationInterface getInstance() {
        IBinder binder = BinderService.getBinder("Application");
        if (binder == null) {
            Log.i("ApplicationBinder: binder missing");
            return null;
        }
        IInterface localInterface = binder.queryLocalInterface(DESCRIPTOR);
        Log.i("ApplicationBinder: binder ready, local=" + (localInterface instanceof ApplicationInterface));
        return !(localInterface instanceof ApplicationInterface) ? new Proxy(binder) : (ApplicationInterface) localInterface;
    }

    @Override
    public java.util.List<String> getRunningApplication() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getProcessesForApp(String packageName, int userId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getNetworkSpeed(String packageName, int userId) {
        throw new UnsupportedOperationException();
    }
}
