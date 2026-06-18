package nep.timeline.cirno.binder;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import nep.timeline.cirno.GlobalVars;
import nep.timeline.cirno.log.Log;

public class BinderService {
    private static final Map<String, IBinder> binders = new HashMap<>();
    private static final long REQUEST_INTERVAL_MS = 1000L;
    private static final String requestToken = UUID.randomUUID().toString();
    private static volatile boolean receiverRegistered = false;
    private static volatile long lastRequestAtMs = 0L;
    private static final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || !GlobalVars.ACTION_BINDER.equals(intent.getAction())) {
                return;
            }
            if (!requestToken.equals(intent.getStringExtra(GlobalVars.EXTRA_BINDER_TOKEN))) {
                Log.w("Binder register: ignored broadcast with invalid token");
                return;
            }
            cacheBinders(intent.getExtras());
        }
    };

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
        Context appContext = context.getApplicationContext();
        if (appContext == null) {
            appContext = context;
        }
        try {
            registerReceiverIfNeeded(appContext);
            requestBinders(appContext);
        } catch (Throwable throwable) {
            Log.e("Binder register failed: registerReceiver", throwable);
        }
    }

    private static void registerReceiverIfNeeded(Context context) {
        if (receiverRegistered) {
            return;
        }
        synchronized (BinderService.class) {
            if (receiverRegistered) {
                return;
            }
            IntentFilter filter = new IntentFilter(GlobalVars.ACTION_BINDER);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
            } else {
                context.registerReceiver(receiver, filter);
            }
            receiverRegistered = true;
            Log.i("Binder register: receiver registered");
        }
    }

    private static void requestBinders(Context context) {
        long now = android.os.SystemClock.uptimeMillis();
        if (now - lastRequestAtMs < REQUEST_INTERVAL_MS) {
            return;
        }
        lastRequestAtMs = now;
        Intent intent = new Intent(GlobalVars.ACTION_BINDER_REQUEST);
        intent.putExtra(GlobalVars.EXTRA_BINDER_TOKEN, requestToken);
        intent.setPackage("android");
        context.sendBroadcast(intent);
    }

    private static void cacheBinders(Bundle extras) {
        if (extras == null) {
            Log.i("Binder register: broadcast has no extras");
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
