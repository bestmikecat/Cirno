package nep.timeline.cirno.virtuals;

import android.os.IBinder;

import nep.timeline.cirno.reflect.CakeReflection;

public record ILocationListener(Object instance) {

    public IBinder asBinder() {
        if (instance == null)
            return null;

        return (IBinder) CakeReflection.callMethod(instance, "asBinder");
    }
}
