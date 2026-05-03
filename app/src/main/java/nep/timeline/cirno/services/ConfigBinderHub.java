package nep.timeline.cirno.services;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.IBinder;
import android.os.ServiceManager;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

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
                List<String> packages = getInstalledPackagesForUserByService(userId);
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
        }
    };

    public static void signalError() {
        SIGNALS.put("error", "1");
    }

    private static List<Integer> getInstalledUserIdsByService() {
        LinkedHashSet<Integer> userIds = new LinkedHashSet<>();
        try {
            IBinder binder = ServiceManager.getService("user");
            if (binder == null) {
                return new ArrayList<>();
            }
            Class<?> stub = Class.forName("android.os.IUserManager$Stub");
            Object ium = stub.getMethod("asInterface", IBinder.class).invoke(null, binder);
            if (ium == null) {
                return new ArrayList<>();
            }

            List<?> users = null;
            Method method = null;
            try {
                method = ium.getClass().getMethod("getUsers", boolean.class, boolean.class, boolean.class);
                users = (List<?>) method.invoke(ium, true, true, true);
            } catch (Throwable ignored) {
            }
            if (users == null) {
                try {
                    method = ium.getClass().getMethod("getUsers", boolean.class, boolean.class);
                    users = (List<?>) method.invoke(ium, true, true);
                } catch (Throwable ignored) {
                }
            }
            if (users == null) {
                try {
                    method = ium.getClass().getMethod("getUsers", boolean.class);
                    users = (List<?>) method.invoke(ium, true);
                } catch (Throwable ignored) {
                }
            }
            if (users == null) {
                try {
                    method = ium.getClass().getMethod("getUsers");
                    users = (List<?>) method.invoke(ium);
                } catch (Throwable ignored) {
                }
            }

            if (users != null) {
                for (Object userObj : users) {
                    int id = extractUserId(userObj);
                    if (id >= 0) {
                        userIds.add(id);
                    }
                }
                Log.d("Config binder IUserManager getUsers success size=" + userIds.size());
            }
        } catch (Throwable e) {
            Log.e("Config binder IUserManager users query failed", e);
        }
        return new ArrayList<>(userIds);
    }

    private static List<String> getInstalledPackagesForUserByService(int userId) {
        LinkedHashSet<String> packages = new LinkedHashSet<>();
        try {
            IBinder binder = ServiceManager.getService("package");
            if (binder == null) {
                return new ArrayList<>();
            }
            Class<?> stub = Class.forName("android.content.pm.IPackageManager$Stub");
            Object ipm = stub.getMethod("asInterface", IBinder.class).invoke(null, binder);
            if (ipm == null) {
                return new ArrayList<>();
            }

            Object resultObj;
            try {
                Method getInstalled = ipm.getClass().getMethod("getInstalledPackages", long.class, int.class);
                resultObj = getInstalled.invoke(ipm, 0L, userId);
            } catch (Throwable ignored) {
                Method getInstalled = ipm.getClass().getMethod("getInstalledPackages", int.class, int.class);
                resultObj = getInstalled.invoke(ipm, 0, userId);
            }

            List<?> list = unwrapListResult(resultObj);
            if (list != null) {
                for (Object item : list) {
                    String pkg = extractPackageName(item);
                    if (pkg != null && !pkg.isEmpty()) {
                        packages.add(pkg);
                    }
                }
                Log.d("Config binder IPackageManager getInstalledPackages success user=" + userId + " size=" + packages.size());
            }
        } catch (Throwable e) {
            Log.e("Config binder IPackageManager packages query failed", e);
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

    private static int extractUserId(Object userObj) {
        if (userObj == null) {
            return -1;
        }
        try {
            Field idField = userObj.getClass().getField("id");
            return idField.getInt(userObj);
        } catch (Throwable ignored) {
        }
        try {
            Method getUserHandle = userObj.getClass().getMethod("getUserHandle");
            Object id = getUserHandle.invoke(userObj);
            if (id instanceof Integer) {
                return (Integer) id;
            }
        } catch (Throwable ignored) {
        }
        return -1;
    }
}
