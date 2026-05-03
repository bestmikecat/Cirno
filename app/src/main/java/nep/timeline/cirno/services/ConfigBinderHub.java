package nep.timeline.cirno.services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nep.timeline.cirno.configs.ConfigManager;
import nep.timeline.cirno.binders.ConfigInterface;
import nep.timeline.cirno.log.Log;

public final class ConfigBinderHub {
    private static final Object LOCK = new Object();
    private static final AtomicReference<String> LAST_ERROR = new AtomicReference<>("");
    private static final Map<String, String> SIGNALS = new ConcurrentHashMap<>();

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
            for (int userId : getInstalledUserIdsByPm()) {
                for (String pkg : getInstalledPackagesForUserByPm(userId)) {
                    if (pkg == null || pkg.isEmpty()) {
                        continue;
                    }
                    result.add(pkg + "#" + userId);
                }
            }
            return new ArrayList<>(result);
        }
    };

    public static void signalError() {
        SIGNALS.put("error", "1");
    }

    private static List<Integer> getInstalledUserIdsByPm() {
        LinkedHashSet<Integer> userIds = new LinkedHashSet<>();
        Pattern userPattern = Pattern.compile("UserInfo\\{(\\d+):");
        List<String> lines = runPmCommand(new String[]{"pm", "list", "users"});
        for (String line : lines) {
            Matcher matcher = userPattern.matcher(line);
            if (matcher.find()) {
                userIds.add(Integer.parseInt(matcher.group(1)));
            }
        }
        if (userIds.isEmpty()) {
            userIds.add(0);
        }
        return new ArrayList<>(userIds);
    }

    private static List<String> getInstalledPackagesForUserByPm(int userId) {
        LinkedHashSet<String> packages = new LinkedHashSet<>();
        List<String> lines = runPmCommand(new String[]{"pm", "list", "packages", "--user", String.valueOf(userId)});
        for (String line : lines) {
            if (line != null && line.startsWith("package:")) {
                String pkg = line.substring("package:".length()).trim();
                if (!pkg.isEmpty()) {
                    packages.add(pkg);
                }
            }
        }
        return new ArrayList<>(packages);
    }

    private static List<String> runPmCommand(String[] command) {
        List<String> out = new ArrayList<>();
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(command);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    out.add(line);
                }
            }
            process.waitFor();
        } catch (Throwable e) {
            Log.e("Config binder pm command failed", e);
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
        return out;
    }
}
