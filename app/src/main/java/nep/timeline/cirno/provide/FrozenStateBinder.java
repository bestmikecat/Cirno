package nep.timeline.cirno.provide;

import android.os.IBinder;
import android.os.IInterface;

import java.util.List;

import nep.timeline.cirno.binder.BinderService;
import nep.timeline.cirno.binders.FrozenStateInterface;
import nep.timeline.cirno.log.Log;

public class FrozenStateBinder extends FrozenStateInterface.Stub {
    public static FrozenStateInterface getInstance() {
        IBinder binder = BinderService.getBinder("FrozenState");
        if (binder == null) {
            Log.i("FrozenStateBinder: binder missing");
            return null;
        }
        IInterface localInterface = binder.queryLocalInterface(DESCRIPTOR);
        Log.i("FrozenStateBinder: binder ready, local=" + (localInterface instanceof FrozenStateInterface));
        return !(localInterface instanceof FrozenStateInterface) ? new Proxy(binder) : (FrozenStateInterface) localInterface;
    }

    @Override
    public String isFrozen(String packageName, int userId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getFrozenStates(List<String> apps) {
        throw new UnsupportedOperationException();
    }
}
