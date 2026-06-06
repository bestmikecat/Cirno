package nep.timeline.cirno.configs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.topjohnwu.superuser.Shell;
import com.topjohnwu.superuser.io.SuFile;

import java.io.File;
import java.io.IOException;

import nep.timeline.cirno.GlobalVars;
import nep.timeline.cirno.configs.settings.ApplicationSettings;
import nep.timeline.cirno.configs.settings.GlobalSettings;
import nep.timeline.cirno.log.Log;
import nep.timeline.cirno.utils.RWUtils;

public class ConfigManagerJson {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
    private final String globalSettingsName = "GlobalSettings.json";
    private final String applicationSettingsName = "ApplicationSettings.json";

    private void ensureApplicationSettingsInitialized() {
        GlobalVars.applicationSettings = ApplicationSettings.ensureInitialized(GlobalVars.applicationSettings);
    }

    private void resetToDefaults() {
        GlobalVars.globalSettings = new GlobalSettings();
        GlobalVars.applicationSettings = ApplicationSettings.ensureInitialized(null);
    }

    private void prepareLogDirSU() {
        Shell.cmd(
                "mkdir -p " + GlobalVars.LOG_DIR,
                "chown system:system " + GlobalVars.CONFIG_DIR + " " + GlobalVars.LOG_DIR,
                "chmod 0770 " + GlobalVars.CONFIG_DIR + " " + GlobalVars.LOG_DIR,
                "[ ! -e " + GlobalVars.LOG_DIR + "/current.log ] || chown system:system " + GlobalVars.LOG_DIR + "/current.log",
                "[ ! -e " + GlobalVars.LOG_DIR + "/current.log ] || chmod 0660 " + GlobalVars.LOG_DIR + "/current.log",
                "[ ! -e " + GlobalVars.LOG_DIR + "/last.log ] || chown system:system " + GlobalVars.LOG_DIR + "/last.log",
                "[ ! -e " + GlobalVars.LOG_DIR + "/last.log ] || chmod 0660 " + GlobalVars.LOG_DIR + "/last.log",
                "restorecon -R " + GlobalVars.CONFIG_DIR + " >/dev/null 2>&1 || true"
        ).exec();
    }

    private void writeConfigByMode(boolean su) {
        try {
            if (su) {
                SuFile configDir = new SuFile(GlobalVars.CONFIG_DIR);
                if (!configDir.exists()) {
                    configDir.mkdir();
                }
                SuFile logDir = new SuFile(GlobalVars.LOG_DIR);
                if (!logDir.exists()) {
                    logDir.mkdir();
                }
                prepareLogDirSU();
            } else {
                File configDir = new File(GlobalVars.CONFIG_DIR);
                if (!configDir.exists()) {
                    configDir.mkdir();
                }
                File logDir = new File(GlobalVars.LOG_DIR);
                if (!logDir.exists()) {
                    logDir.mkdir();
                }
            }

            GlobalSettings globalSettings = GlobalVars.globalSettings;
            if (globalSettings != null) {
                String globalConfigStr = gson.toJson(globalSettings);
                if (su) {
                    RWUtils.writeStringToFileSU(new SuFile(GlobalVars.CONFIG_DIR, globalSettingsName), globalConfigStr, false);
                } else {
                    RWUtils.writeStringToFile(new File(GlobalVars.CONFIG_DIR, globalSettingsName), globalConfigStr);
                }
            }

            ApplicationSettings applicationSettings = GlobalVars.applicationSettings;
            if (applicationSettings != null) {
                String applicationConfigStr = gson.toJson(applicationSettings);
                if (su) {
                    RWUtils.writeStringToFileSU(new SuFile(GlobalVars.CONFIG_DIR, applicationSettingsName), applicationConfigStr, false);
                } else {
                    RWUtils.writeStringToFile(new File(GlobalVars.CONFIG_DIR, applicationSettingsName), applicationConfigStr);
                }
            }
        } catch (IOException e) {
            Log.e("Save Config", e);
        }
    }

    public void readConfig() {
        try {
            File globalFile = new File(GlobalVars.CONFIG_DIR, globalSettingsName);
            if (!globalFile.exists()) {
                GlobalVars.globalSettings = new GlobalSettings();
                saveConfig();
            } else {
                String globalData = RWUtils.readConfig(GlobalVars.CONFIG_DIR + "/" + globalSettingsName);
                GlobalVars.globalSettings = GlobalSettings.ensureInitialized(gson.fromJson(globalData, GlobalSettings.class));
            }
            File applicationFile = new File(GlobalVars.CONFIG_DIR, applicationSettingsName);
            if (!applicationFile.exists()) {
                GlobalVars.applicationSettings = new ApplicationSettings();
                saveConfig();
            } else {
                String applicationData = RWUtils.readConfig(GlobalVars.CONFIG_DIR + "/" + applicationSettingsName);
                GlobalVars.applicationSettings = gson.fromJson(applicationData, ApplicationSettings.class);
                if (GlobalVars.applicationSettings == null) {
                    GlobalVars.applicationSettings = new ApplicationSettings();
                    saveConfig();
                }
            }
            ensureApplicationSettingsInitialized();
        } catch (JsonSyntaxException | JsonIOException e) {
            resetToDefaults();
            saveConfig();
        }
    }

    public void saveConfig() {
        writeConfigByMode(false);
    }

    public boolean readConfigSU() {
        boolean read = true;
        try {
            SuFile globalFile = new SuFile(GlobalVars.CONFIG_DIR, globalSettingsName);
            if (!globalFile.exists()) {
                GlobalVars.globalSettings = new GlobalSettings();
                read = false;
            } else {
                String globalData = RWUtils.readConfig(globalFile);
                GlobalVars.globalSettings = GlobalSettings.ensureInitialized(gson.fromJson(globalData, GlobalSettings.class));
            }
            SuFile applicationFile = new SuFile(GlobalVars.CONFIG_DIR, applicationSettingsName);
            if (!applicationFile.exists()) {
                GlobalVars.applicationSettings = new ApplicationSettings();
                read = false;
            } else {
                String applicationData = RWUtils.readConfig(applicationFile);
                GlobalVars.applicationSettings = gson.fromJson(applicationData, ApplicationSettings.class);
                if (GlobalVars.applicationSettings == null) {
                    GlobalVars.applicationSettings = new ApplicationSettings();
                    read = false;
                }
            }
            ensureApplicationSettingsInitialized();
        } catch (JsonSyntaxException | JsonIOException e) {
            resetToDefaults();
            return false;
        }
        return read;
    }

    public void saveConfigSU() {
        writeConfigByMode(true);
    }

    public String dumpGlobalSettingsJson() {
        GlobalSettings settings = GlobalSettings.ensureInitialized(GlobalVars.globalSettings);
        GlobalVars.globalSettings = settings;
        return gson.toJson(settings);
    }

    public String dumpApplicationSettingsJson() {
        ApplicationSettings settings = ApplicationSettings.ensureInitialized(GlobalVars.applicationSettings);
        GlobalVars.applicationSettings = settings;
        return gson.toJson(settings);
    }

    public boolean applyGlobalSettingsJson(String json) {
        GlobalSettings oldSettings = GlobalVars.globalSettings;
        try {
            GlobalSettings settings = gson.fromJson(json, GlobalSettings.class);
            GlobalVars.globalSettings = GlobalSettings.ensureInitialized(settings);
            saveConfig();
            return true;
        } catch (Throwable e) {
            GlobalVars.globalSettings = oldSettings;
            Log.e("Apply GlobalSettings", e);
            return false;
        }
    }

    public boolean applyGlobalSettingsJsonSU(String json) {
        GlobalSettings oldSettings = GlobalVars.globalSettings;
        try {
            GlobalSettings settings = gson.fromJson(json, GlobalSettings.class);
            GlobalVars.globalSettings = GlobalSettings.ensureInitialized(settings);
            saveConfigSU();
            return true;
        } catch (Throwable e) {
            GlobalVars.globalSettings = oldSettings;
            Log.e("Apply GlobalSettings", e);
            return false;
        }
    }

    public boolean applyApplicationSettingsJson(String json) {
        ApplicationSettings oldSettings = GlobalVars.applicationSettings;
        try {
            ApplicationSettings settings = gson.fromJson(json, ApplicationSettings.class);
            GlobalVars.applicationSettings = ApplicationSettings.ensureInitialized(settings);
            saveConfig();
            return true;
        } catch (Throwable e) {
            GlobalVars.applicationSettings = oldSettings;
            Log.e("Apply ApplicationSettings", e);
            return false;
        }
    }

    public boolean applyApplicationSettingsJsonSU(String json) {
        ApplicationSettings oldSettings = GlobalVars.applicationSettings;
        try {
            ApplicationSettings settings = gson.fromJson(json, ApplicationSettings.class);
            GlobalVars.applicationSettings = ApplicationSettings.ensureInitialized(settings);
            saveConfigSU();
            return true;
        } catch (Throwable e) {
            GlobalVars.applicationSettings = oldSettings;
            Log.e("Apply ApplicationSettings", e);
            return false;
        }
    }
}
