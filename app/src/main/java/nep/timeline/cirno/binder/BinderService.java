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

    public static int binderCount() {
        synchronized (binders) {
            return binders.size();
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    public static void register(Context context) {
        if (context == null) {
            Log.w("Binder register skipped: context is null");
            return;
        }
        Intent intent;
        try {
            intent = context.registerReceiver(null, new IntentFilter(GlobalVars.TAG + "-Binder"));
        } catch (Throwable throwable) {
            Log.e("Binder register failed: registerReceiver", throwable);
            return;
        }
        Bundle extras = intent == null ? null : intent.getExtras();
        if (extras == null) {
            Log.i("Binder register: no sticky broadcast yet");
            return;
        }
        int count = 0;
        int valid = 0;
        synchronized (binders) {
            for (String name : extras.keySet()) {
                IBinder binder = extras.getBinder(name);
                binders.put(name, binder);
                count++;
                if (binder != null) {
                    valid++;
                }
            }
        }
        Log.i("Binder register: cached " + valid + "/" + count + " binder(s), total=" + binderCount());
    }
}
