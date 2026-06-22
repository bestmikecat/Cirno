package nep.timeline.cirno.binder;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;

import nep.timeline.cirno.IBinderManager;
import nep.timeline.cirno.IStatusInterface;
import nep.timeline.cirno.IApplicationInterface;
import nep.timeline.cirno.IFrozenStateInterface;
import nep.timeline.cirno.GlobalVars;
import nep.timeline.cirno.log.Log;

public class BinderService {
    private static IBinderManager sManager;
    private static volatile boolean receiverRegistered = false;

    private static final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || !GlobalVars.ACTION_BINDER.equals(intent.getAction())) {
                return;
            }
            Log.i("BinderService: broadcast received");
            Bundle extras = intent.getExtras();
            if (extras == null) {
                Log.w("BinderService: broadcast has no extras");
                return;
            }
            IBinder binder = extras.getBinder("Manager");
            if (binder == null) {
                Log.w("BinderService: manager binder missing in broadcast");
                return;
            }
            sManager = IBinderManager.Stub.asInterface(binder);
            Log.i("BinderService: obtained manager from broadcast");
        }
    };

    public static void register(Context appContext) {
        if (appContext == null) {
            return;
        }
        try {
            registerReceiverIfNeeded(appContext);
            requestBinders(appContext);
        } catch (Throwable e) {
            Log.e("BinderService: register failed", e);
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
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
            Log.i("BinderService: receiver registered");
        }
    }

    private static void requestBinders(Context context) {
        Intent intent = new Intent(GlobalVars.ACTION_BINDER_REQUEST);
        intent.setPackage("android");
        context.sendBroadcast(intent);
    }

    private static IStatusInterface fetchStatusBinder() {
        if (sManager == null) {
            Log.w("BinderService: manager is null, binder not yet received");
            return null;
        }
        try {
            return sManager.getStatusBinder();
        } catch (Throwable e) {
            Log.w("BinderService: failed to get status binder", e);
            sManager = null;
        }
        return null;
    }

    private static IApplicationInterface fetchApplicationBinder() {
        if (sManager == null) {
            Log.w("BinderService: manager is null, binder not yet received");
            return null;
        }
        try {
            return sManager.getApplicationBinder();
        } catch (Throwable e) {
            Log.w("BinderService: failed to get application binder", e);
            sManager = null;
        }
        return null;
    }

    private static IFrozenStateInterface fetchFrozenStateBinder() {
        if (sManager == null) {
            Log.w("BinderService: manager is null, binder not yet received");
            return null;
        }
        try {
            return sManager.getFrozenStateBinder();
        } catch (Throwable e) {
            Log.w("BinderService: failed to get frozen state binder", e);
            sManager = null;
        }
        return null;
    }

    public static IStatusInterface getStatusBinder() {
        IStatusInterface binder = fetchStatusBinder();
        if (binder == null) {
            Log.w("BinderService: status binder missing");
        }
        return binder;
    }

    public static IApplicationInterface getApplicationBinder() {
        IApplicationInterface binder = fetchApplicationBinder();
        if (binder == null) {
            Log.w("BinderService: application binder missing");
        }
        return binder;
    }

    public static IFrozenStateInterface getFrozenStateBinder() {
        IFrozenStateInterface binder = fetchFrozenStateBinder();
        if (binder == null) {
            Log.w("BinderService: frozen state binder missing");
        }
        return binder;
    }
}
