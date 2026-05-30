package nep.timeline.cirno.services;

import org.apache.commons.io.FileUtils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Process;
import android.os.UserHandle;
import android.os.UserManager;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nep.timeline.cirno.BuildConfig;
import nep.timeline.cirno.GlobalVars;
import nep.timeline.cirno.configs.ConfigManager;
import nep.timeline.cirno.binders.ConfigInterface;
import nep.timeline.cirno.log.Log;
import nep.timeline.cirno.utils.RWUtils;

public final class ConfigBinderHub {
    private static final Object LOCK = new Object();
    private static final AtomicReference<String> LAST_ERROR = new AtomicReference<>("");
    private static final Map<String, String> SIGNALS = new ConcurrentHashMap<>();
    public static final String SIGNAL_ANDROID_HOOK_READY = "android_hook_ready";
    public static final String SIGNAL_SYSTEMUI_HOOK_READY = "systemui_hook_ready";
    private static volatile long lastManagedAppsLogAtMs = 0L;

    private ConfigBinderHub() {
    }

    public static final ConfigInterface.Stub configBinder = new ConfigInterface.Stub() {
        @Override
        public String getGlobalSettingsJson() {
            if (!isTrustedCaller()) {
                return "";
            }
            synchronized (LOCK) {
                return ConfigManager.manager.dumpGlobalSettingsJson();
            }
        }

        @Override
        public String getApplicationSettingsJson() {
            if (!isTrustedCaller()) {
                return "";
            }
            synchronized (LOCK) {
                return ConfigManager.manager.dumpApplicationSettingsJson();
            }
        }

        @Override
        public boolean setGlobalSettingsJson(String json) {
            if (!isTrustedCaller()) {
                return false;
            }
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
            if (!isTrustedCaller()) {
                return false;
            }
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
            if (!isTrustedCaller()) {
                return "";
            }
            return LAST_ERROR.get();
        }

        @Override
        public boolean setSignal(String key, String value) {
            if (!isTrustedCaller()) {
                return false;
            }
            if (key == null || key.isEmpty()) {
                return false;
            }
            SIGNALS.put(key, value == null ? "" : value);
            return true;
        }

        @Override
        public String getSignal(String key) {
            if (!isTrustedCaller()) {
                return "";
            }
            if (key == null || key.isEmpty()) {
                return "";
            }
            String value = SIGNALS.get(key);
            return value == null ? "" : value;
        }

        @Override
        public List<String> getManagedAppKeys() {
            if (!isTrustedCaller()) {
                return new ArrayList<>();
            }
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
            if (!isTrustedCaller()) {
                return "";
            }
            return BuildConfig.VERSION_NAME;
        }

        @Override
        public String getLogContent() {
            if (!isTrustedCaller()) {
                return "";
            }
            File logFile = new File(GlobalVars.LOG_DIR, "current.log");
            if (!logFile.exists()) {
                return "";
            }
            return RWUtils.readConfig(logFile.getAbsolutePath());
        }

        @Override
        public String getLogContentPage(int startLine, int lineCount) {
            if (!isTrustedCaller()) {
                return "";
            }
            File logFile = new File(GlobalVars.LOG_DIR, "current.log");
            if (!logFile.exists() || lineCount <= 0) {
                return "";
            }

            int safeStartLine = Math.max(0, startLine);
            try {
                List<String> lines = FileUtils.readLines(logFile, StandardCharsets.UTF_8);
                if (safeStartLine >= lines.size()) {
                    return "";
                }
                int endLine = Math.min(lines.size(), safeStartLine + lineCount);
                return String.join("\n", lines.subList(safeStartLine, endLine));
            } catch (IOException e) {
                Log.e("Config binder read log page failed", e);
                return "";
            }
        }

        @Override
        public boolean isReKernelAvailable() {
            if (!isTrustedCaller()) {
                return false;
            }
            if (BinderService.received) {
                return true;
            }
            return new File("/proc/rekernel").exists();
        }
    };

    public static void signalError() {
        SIGNALS.put("error", "1");
    }

    public static void setSignal(String key, String value) {
        if (key == null || key.isEmpty()) {
            return;
        }
        SIGNALS.put(key, value == null ? "" : value);
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

    private static boolean isTrustedCaller() {
        int callingUid = Binder.getCallingUid();
        if (callingUid == Process.myUid() || callingUid == Process.SYSTEM_UID || callingUid == Process.ROOT_UID) {
            return true;
        }

        Context context = ActivityManagerService.getContext();
        if (context == null || context.getPackageManager() == null) {
            Log.w("Config binder rejected caller: context unavailable, uid=" + callingUid);
            return false;
        }

        try {
            PackageManager packageManager = context.getPackageManager();
            String[] packages = packageManager.getPackagesForUid(callingUid);
            if (packages == null) {
                Log.w("Config binder rejected caller: no packages, uid=" + callingUid);
                return false;
            }
            for (String packageName : packages) {
                if (BuildConfig.APPLICATION_ID.equals(packageName)) {
                    return true;
                }
                if (packageManager.checkSignatures(BuildConfig.APPLICATION_ID, packageName) == PackageManager.SIGNATURE_MATCH) {
                    return true;
                }
            }
        } catch (Throwable e) {
            Log.w("Config binder caller check failed, uid=" + callingUid, e);
        }
        Log.w("Config binder rejected caller uid=" + callingUid);
        return false;
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
