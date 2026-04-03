package nep.timeline.cirno.virtuals;

import android.os.IBinder;

import de.robv.android.xposed.XposedHelpers;

public record ILocationListener(Object instance) {

    public IBinder asBinder() {
        if (instance == null)
            return null;

        return (IBinder) XposedHelpers.callMethod(instance, "asBinder");
    }
}
