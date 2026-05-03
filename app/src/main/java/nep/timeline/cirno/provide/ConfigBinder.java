package nep.timeline.cirno.provide;

import android.os.IBinder;
import android.os.IInterface;

import nep.timeline.cirno.binder.BinderService;
import nep.timeline.cirno.binders.ConfigInterface;
import nep.timeline.cirno.log.Log;

public class ConfigBinder extends ConfigInterface.Stub {
    public static ConfigInterface getInstance() {
        IBinder binder = BinderService.getBinder("Config");
        if (binder == null) {
            Log.i("ConfigBinder: binder missing");
            return null;
        }
        IInterface localInterface = binder.queryLocalInterface(DESCRIPTOR);
        Log.i("ConfigBinder: binder ready, local=" + (localInterface instanceof ConfigInterface));
        return !(localInterface instanceof ConfigInterface) ? new Proxy(binder) : (ConfigInterface) localInterface;
    }

    @Override
    public String getGlobalSettingsJson() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getApplicationSettingsJson() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean setGlobalSettingsJson(String json) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean setApplicationSettingsJson(String json) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getLastError() {
        throw new UnsupportedOperationException();
    }
}
