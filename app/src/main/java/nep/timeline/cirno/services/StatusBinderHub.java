package nep.timeline.cirno.services;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import nep.timeline.cirno.BuildConfig;
import nep.timeline.cirno.provide.StatusBinderFacade;
import nep.timeline.cirno.reflect.CakeHooker;
import nep.timeline.cirno.utils.SystemChecker;

public final class StatusBinderHub {
    public static final String SIGNAL_DEVICE_TYPE = "device_type";
    public static final String SIGNAL_ADD_ON_REQUIRED = "add_on_required";
    public static final String SIGNAL_HOOK_TYPE = "hook_type";
    private static final Map<String, String> SIGNALS = new ConcurrentHashMap<>();

    private StatusBinderHub() {
    }

    public static final StatusBinderFacade statusBinder = new StatusBinderFacade() {
        @Override
        public String getSignal(String key) {
            return StatusBinderHub.getSignal(key);
        }

        @Override
        public boolean isPacketAvailable() {
            if (BinderService.received) {
                return true;
            }
            return new File("/proc/rekernel").exists() || SystemChecker.isOplus(CakeHooker.getHostClassLoader()) || GreezeManagerServiceWrapper.instance != null;
        }

        @Override
        public String getHookVersion() {
            return BuildConfig.VERSION_NAME;
        }
    };

    public static void signalError() {
        SIGNALS.put("error", "1");
    }

    public static boolean setSignal(String key, String value) {
        if (key == null || key.isEmpty()) {
            return false;
        }
        SIGNALS.put(key, value == null ? "" : value);
        return true;
    }

    public static String getSignal(String key) {
        if (key == null || key.isEmpty()) {
            return "";
        }
        String value = SIGNALS.get(key);
        return value == null ? "" : value;
    }
}
