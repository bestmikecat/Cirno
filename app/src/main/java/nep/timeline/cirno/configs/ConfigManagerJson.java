package nep.timeline.cirno.configs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

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

    private void writeConfig() {
        try {
            GlobalSettings globalSettings = GlobalVars.globalSettings;
            if (globalSettings != null) {
                String globalConfigStr = gson.toJson(globalSettings);
                RWUtils.writeStringToFile(new File(GlobalVars.CONFIG_DIR, globalSettingsName), globalConfigStr);
            }

            ApplicationSettings applicationSettings = GlobalVars.applicationSettings;
            if (applicationSettings != null) {
                String applicationConfigStr = gson.toJson(applicationSettings);
                RWUtils.writeStringToFile(new File(GlobalVars.CONFIG_DIR, applicationSettingsName), applicationConfigStr);
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
        writeConfig();
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
}
