package nep.timeline.cirno.services;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Binder;
import android.os.UserHandle;
import android.os.UserManager;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nep.timeline.cirno.BuildConfig;
import nep.timeline.cirno.configs.ConfigManager;
import nep.timeline.cirno.binders.ConfigInterface;
import nep.timeline.cirno.log.Log;

public final class ConfigBinderHub {
    private static final Object LOCK = new Object();
    private static final AtomicReference<String> LAST_ERROR = new AtomicReference<>("");
    private static final Map<String, String> SIGNALS = new ConcurrentHashMap<>();
    private static volatile long lastManagedAppsLogAtMs = 0L;

    private ConfigBinderHub() {
    }

    public static final ConfigInterface.Stub configBinder = new ConfigInterface.Stub() {
        @Override
        public String getGlobalSettingsJson() {
            synchronized (LOCK) {
                return ConfigManager.manager.dumpGlobalSettingsJson();
            }
        }

        @Override
        public String getApplicationSettingsJson() {
            synchronized (LOCK) {
                return ConfigManager.manager.dumpApplicationSettingsJson();
            }
        }

        @Override
        public boolean setGlobalSettingsJson(String json) {
            synchronized (LOCK) {
                boolean ok = ConfigManager.manager.applyGlobalSettingsJson(json);
                if (!ok) {
                    LAST_ERROR.set("更新全局配置失败");
                    Log.e("Config binder: global settings update failed");
                    return false;
                }
                LAST_ERROR.set("");
                Log.i("Config binder: global settings updated");
                return true;
            }
        }

        @Override
        public boolean setApplicationSettingsJson(String json) {
            synchronized (LOCK) {
                boolean ok = ConfigManager.manager.applyApplicationSettingsJson(json);
                if (!ok) {
                    LAST_ERROR.set("更新应用配置失败");
                    Log.e("Config binder: application settings update failed");
                    return false;
                }
                LAST_ERROR.set("");
                Log.i("Config binder: application settings updated");
                return true;
            }
        }

        @Override
        public String getLastError() {
            return LAST_ERROR.get();
        }

        @Override
        public boolean setSignal(String key, String value) {
            if (key == null || key.isEmpty()) {
                return false;
            }
            SIGNALS.put(key, value == null ? "" : value);
            return true;
        }

        @Override
        public String getSignal(String key) {
            if (key == null || key.isEmpty()) {
                return "";
            }
            String value = SIGNALS.get(key);
            return value == null ? "" : value;
        }

        @Override
        public List<String> getManagedAppKeys() {
            long token = Binder.clearCallingIdentity();
            try {
                LinkedHashSet<String> result = new LinkedHashSet<>();
                Context context = ActivityManagerService.getContext();
                if (context == null) {
                    Log.w("Config binder managed apps skipped: context unavailable");
                    return new ArrayList<>();
                }
                List<Integer> userIds = getInstalledUserIdsByService();
                if (userIds.isEmpty()) {
                    userIds.add(0);
                    Log.d("Config binder users fallback=user0");
                }
                int packageCount = 0;
                for (int userId : userIds) {
                    List<String> packages = getInstalledPackagesForUserByFramework(context, userId);
                    packageCount += packages.size();
                    for (String pkg : packages) {
                        if (pkg == null || pkg.isEmpty()) {
                            continue;
                        }
                        result.add(pkg + "#" + userId);
                    }
                }
                long now = System.currentTimeMillis();
                if (now - lastManagedAppsLogAtMs > 30000L) {
                    lastManagedAppsLogAtMs = now;
                    Log.i("Config binder managed apps: users=" + userIds.size() + ", packages=" + packageCount + ", keys=" + result.size());
                }
                return new ArrayList<>(result);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        @Override
        public String getModuleVersion() {
            return BuildConfig.VERSION_NAME;
        }
    };

    public static void signalError() {
        SIGNALS.put("error", "1");
    }

    public static void readConfigSynchronized() {
        synchronized (LOCK) {
            ConfigManager.manager.readConfig();
        }
    }

    private static List<Integer> getInstalledUserIdsByService() {
        LinkedHashSet<Integer> userIds = new LinkedHashSet<>();
        try {
            UserManager userManager = (UserManager) ActivityManagerService.getContext().getSystemService(Context.USER_SERVICE);
            if (userManager == null) {
                return new ArrayList<>();
            }
            List<UserHandle> profiles = userManager.getUserProfiles();
            if (profiles != null) {
                for (UserHandle profile : profiles) {
                    if (profile != null) {
                        userIds.add(profile.hashCode());
                    }
                }
            }
            Log.d("Config binder UserManager getUserProfiles success size=" + userIds.size());
        } catch (Throwable e) {
            Log.e("Config binder UserManager users query failed", e);
        }
        return new ArrayList<>(userIds);
    }

    private static List<String> getInstalledPackagesForUserByFramework(Context context, int userId) {
        LinkedHashSet<String> packages = new LinkedHashSet<>();
        try {
            Object resultObj;
            if (context.getPackageManager() == null) {
                Log.e("Config binder getInstalledPackagesAsUser failed: PackageManager is null, userId=" + userId);
                return new ArrayList<>();
            }
            Method getInstalledAsUser = context.getPackageManager().getClass().getMethod("getInstalledPackagesAsUser", int.class, int.class);
            resultObj = getInstalledAsUser.invoke(context.getPackageManager(), 0, userId);
            List<?> list = unwrapListResult(resultObj);
            if (list != null) {
                for (Object item : list) {
                    String pkg = extractPackageName(item);
                    if (pkg != null && !pkg.isEmpty()) {
                        packages.add(pkg);
                    }
                }
                Log.d("Config binder getInstalledPackagesAsUser success user=" + userId + " size=" + packages.size());
            }
        } catch (Throwable e) {
            Log.e("Config binder getInstalledPackagesAsUser failed, userId=" + userId, e);
        }
        return new ArrayList<>(packages);
    }

    private static List<?> unwrapListResult(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof List) {
            return (List<?>) obj;
        }
        try {
            Method getList = obj.getClass().getMethod("getList");
            Object listObj = getList.invoke(obj);
            if (listObj instanceof List) {
                return (List<?>) listObj;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static String extractPackageName(Object pkgObj) {
        if (pkgObj == null) {
            return null;
        }
        if (pkgObj instanceof PackageInfo) {
            return ((PackageInfo) pkgObj).packageName;
        }
        try {
            Field field = pkgObj.getClass().getField("packageName");
            Object value = field.get(pkgObj);
            return value instanceof String ? (String) value : null;
        } catch (Throwable ignored) {
        }
        try {
            Method method = pkgObj.getClass().getMethod("getPackageName");
            Object value = method.invoke(pkgObj);
            return value instanceof String ? (String) value : null;
        } catch (Throwable ignored) {
        }
        return null;
    }

}
