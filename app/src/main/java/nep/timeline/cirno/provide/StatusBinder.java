package nep.timeline.cirno.provide;

import android.os.IBinder;
import android.os.IInterface;

import nep.timeline.cirno.binder.BinderService;
import nep.timeline.cirno.binders.StatusInterface;
import nep.timeline.cirno.log.Log;

public class StatusBinder extends StatusInterface.Stub {
    public static StatusInterface getInstance() {
        IBinder binder = BinderService.getBinder("Status");
        if (binder == null) {
            Log.i("StatusBinder: binder missing");
            return null;
        }
        IInterface localInterface = binder.queryLocalInterface(DESCRIPTOR);
        Log.i("StatusBinder: binder ready, local=" + (localInterface instanceof StatusInterface));
        return !(localInterface instanceof StatusInterface) ? new Proxy(binder) : (StatusInterface) localInterface;
    }

    @Override
    public String getSignal(String key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isPacketAvailable() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getHookVersion() {
        throw new UnsupportedOperationException();
    }
}
