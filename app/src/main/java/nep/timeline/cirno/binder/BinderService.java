package nep.timeline.cirno.binder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;

import java.util.HashMap;
import java.util.Map;

import nep.timeline.cirno.GlobalVars;
import nep.timeline.cirno.log.Log;

public class BinderService {
    private static final Map<String, IBinder> binders = new HashMap<>();

    public static IBinder getBinder(String name) {
        synchronized (binders) {
            return binders.get(name);
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    public static void register(Context context) {
        Intent intent = context.registerReceiver(null, new IntentFilter(GlobalVars.TAG + "-Binder"));
        if (intent == null) {
            Log.i("Binder register: no sticky broadcast yet");
            return;
        }
        Bundle extras = intent.getExtras();
        if (extras == null) {
            Log.i("Binder register: sticky broadcast has no extras");
            return;
        }
        int count = 0;
        synchronized (binders) {
            for (String name : extras.keySet()) {
                binders.put(name, extras.getBinder(name));
                count++;
            }
        }
        Log.i("Binder register: cached " + count + " binder(s)");
    }
}
