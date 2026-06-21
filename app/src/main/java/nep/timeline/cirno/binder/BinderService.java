package nep.timeline.cirno.binder;

import android.os.IBinder;

import java.lang.reflect.Method;

import nep.timeline.cirno.IBinderManager;
import nep.timeline.cirno.IStatusInterface;
import nep.timeline.cirno.IApplicationInterface;
import nep.timeline.cirno.IFrozenStateInterface;
import nep.timeline.cirno.log.Log;

public class BinderService {
    private static final String SERVICE_NAME = "cirno";
    private static final long RETRY_POLL_MS = 200L;
    private static final int MAX_RETRY_COUNT = 15;

    private static volatile IBinderManager sManager;
    private static volatile IStatusInterface sStatusBinder;
    private static volatile IApplicationInterface sApplicationBinder;
    private static volatile IFrozenStateInterface sFrozenStateBinder;

    public static void register(android.content.Context appContext) {
        fetchManager();
    }

    private static void fetchManager() {
        try {
            Class<?> smClass = Class.forName("android.os.ServiceManager");
            Method getService = smClass.getMethod("getService", String.class);
            IBinder binder = (IBinder) getService.invoke(null, SERVICE_NAME);
            if (binder == null) {
                Log.w("BinderService: manager not found in ServiceManager");
                return;
            }
            sManager = IBinderManager.Stub.asInterface(binder);
            Log.i("BinderService: obtained manager from ServiceManager");

            sStatusBinder = sManager.getStatusBinder();
            sApplicationBinder = sManager.getApplicationBinder();
            sFrozenStateBinder = sManager.getFrozenStateBinder();
            Log.i("BinderService: pre-fetched all binders");
        } catch (Throwable e) {
            Log.e("BinderService: failed to get manager from ServiceManager", e);
        }
    }

    private static boolean isManagerAlive() {
        IBinderManager mgr = sManager;
        return mgr != null && mgr.asBinder().pingBinder();
    }

    public static IStatusInterface getStatusBinder() {
        if (sStatusBinder != null && sStatusBinder.asBinder().pingBinder()) {
            return sStatusBinder;
        }
        for (int i = 0; i < MAX_RETRY_COUNT; i++) {
            if (!isManagerAlive()) {
                sManager = null;
                fetchManager();
            }
            if (sManager != null) {
                try {
                    sStatusBinder = sManager.getStatusBinder();
                    if (sStatusBinder != null) {
                        Log.i("BinderService: status binder obtained after retry " + (i + 1));
                        return sStatusBinder;
                    }
                } catch (Throwable e) {
                    Log.w("BinderService: status retry " + (i + 1) + " failed", e);
                    sManager = null;
                }
            }
            sleep(RETRY_POLL_MS);
        }
        Log.w("BinderService: failed to get status binder after " + MAX_RETRY_COUNT + " retries");
        return null;
    }

    public static IApplicationInterface getApplicationBinder() {
        if (sApplicationBinder != null && sApplicationBinder.asBinder().pingBinder()) {
            return sApplicationBinder;
        }
        for (int i = 0; i < MAX_RETRY_COUNT; i++) {
            if (!isManagerAlive()) {
                sManager = null;
                fetchManager();
            }
            if (sManager != null) {
                try {
                    sApplicationBinder = sManager.getApplicationBinder();
                    if (sApplicationBinder != null) {
                        Log.i("BinderService: application binder obtained after retry " + (i + 1));
                        return sApplicationBinder;
                    }
                } catch (Throwable e) {
                    Log.w("BinderService: application retry " + (i + 1) + " failed", e);
                    sManager = null;
                }
            }
            sleep(RETRY_POLL_MS);
        }
        Log.w("BinderService: failed to get application binder after " + MAX_RETRY_COUNT + " retries");
        return null;
    }

    public static IFrozenStateInterface getFrozenStateBinder() {
        if (sFrozenStateBinder != null && sFrozenStateBinder.asBinder().pingBinder()) {
            return sFrozenStateBinder;
        }
        for (int i = 0; i < MAX_RETRY_COUNT; i++) {
            if (!isManagerAlive()) {
                sManager = null;
                fetchManager();
            }
            if (sManager != null) {
                try {
                    sFrozenStateBinder = sManager.getFrozenStateBinder();
                    if (sFrozenStateBinder != null) {
                        Log.i("BinderService: frozen state binder obtained after retry " + (i + 1));
                        return sFrozenStateBinder;
                    }
                } catch (Throwable e) {
                    Log.w("BinderService: frozen state retry " + (i + 1) + " failed", e);
                    sManager = null;
                }
            }
            sleep(RETRY_POLL_MS);
        }
        Log.w("BinderService: failed to get frozen state binder after " + MAX_RETRY_COUNT + " retries");
        return null;
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
