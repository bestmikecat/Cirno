package nep.timeline.cirno.provide;

import android.os.IBinder;
import android.os.IInterface;

import nep.timeline.cirno.binder.BinderService;
import nep.timeline.cirno.binders.FrozenStateInterface;

public class FrozenStateBinder extends FrozenStateInterface.Stub {
    public static FrozenStateInterface getInstance() {
        IBinder binder = BinderService.getBinder("FrozenState");
        if (binder == null) {
            return null;
        }
        IInterface localInterface = binder.queryLocalInterface(DESCRIPTOR);
        return !(localInterface instanceof FrozenStateInterface) ? new Proxy(binder) : (FrozenStateInterface) localInterface;
    }

    @Override
    public String isFrozen(String packageName, int userId) {
        throw new UnsupportedOperationException();
    }
}
