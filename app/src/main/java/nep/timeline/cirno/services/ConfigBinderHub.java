package nep.timeline.cirno.services;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.UserManager;

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
            List<Integer> userIds = getInstalledUserIdsByFramework(context);
            int packageCount = 0;
            for (int userId : userIds) {
                List<String> packages = getInstalledPackagesForUserByFramework(context.getPackageManager(), userId);
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

    private static List<Integer> getInstalledUserIdsByFramework(Context context) {
        LinkedHashSet<Integer> userIds = new LinkedHashSet<>();
        try {
            UserManager userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
            if (userManager != null) {
                try {
                    Method getUsers = userManager.getClass().getMethod("getUsers");
                    Object usersObj = getUsers.invoke(userManager);
                    if (usersObj instanceof List) {
                        for (Object user : (List<?>) usersObj) {
                            int id = extractUserId(user);
                            if (id >= 0) {
                                userIds.add(id);
                            }
                        }
                    }
                } catch (Throwable ignored) {
                    Method getUserHandles = userManager.getClass().getMethod("getUserHandles", boolean.class);
                    Object handlesObj = getUserHandles.invoke(userManager, true);
                    if (handlesObj instanceof List) {
                        for (Object handle : (List<?>) handlesObj) {
                            int id = (int) handle.getClass().getMethod("getIdentifier").invoke(handle);
                            userIds.add(id);
                        }
                    }
                }
            }
        } catch (Throwable e) {
            Log.e("Config binder users query failed", e);
        }
        if (userIds.isEmpty()) {
            userIds.add(0);
        }
        return new ArrayList<>(userIds);
    }

    private static List<String> getInstalledPackagesForUserByFramework(PackageManager packageManager, int userId) {
        LinkedHashSet<String> packages = new LinkedHashSet<>();
        try {
            Method getInstalledAsUser = packageManager.getClass().getMethod("getInstalledPackagesAsUser", int.class, int.class);
            Object pkgObj = getInstalledAsUser.invoke(packageManager, PackageManager.GET_META_DATA, userId);
            if (pkgObj instanceof List) {
                for (Object item : (List<?>) pkgObj) {
                    if (item instanceof PackageInfo) {
                        String pkg = ((PackageInfo) item).packageName;
                        if (pkg != null && !pkg.isEmpty()) {
                            packages.add(pkg);
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
            if (userId == 0) {
                try {
                    List<PackageInfo> list = packageManager.getInstalledPackages(PackageManager.GET_META_DATA);
                    for (PackageInfo info : list) {
                        if (info != null && info.packageName != null && !info.packageName.isEmpty()) {
                            packages.add(info.packageName);
                        }
                    }
                } catch (Throwable e) {
                    Log.e("Config binder packages query failed", e);
                }
            } else {
                Log.w("Config binder packages query fallback: user " + userId + " returned no packages");
            }
        }
        return new ArrayList<>(packages);
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
