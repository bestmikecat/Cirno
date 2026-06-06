package nep.timeline.cirno.configs;

public class ConfigManager {
    public static final ConfigManagerJson manager = new ConfigManagerJson();

    public static ConfigManagerJson.ReadResult readConfig() {
        return manager.readConfig();
    }

    public static void saveConfig() {
        manager.saveConfig();
        manager.readConfig();
    }
}
