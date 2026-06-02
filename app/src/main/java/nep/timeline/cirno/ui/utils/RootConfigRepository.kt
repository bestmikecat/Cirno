package nep.timeline.cirno.ui.utils

import nep.timeline.cirno.GlobalVars
import nep.timeline.cirno.configs.ConfigManager
import nep.timeline.cirno.configs.settings.ApplicationSettings
import nep.timeline.cirno.configs.settings.GlobalSettings

object RootConfigRepository {
    private val gson = com.google.gson.Gson()
    private var lastError: String = ""

    fun loadIntoMemory(): Boolean {
        return try {
            if (!ConfigManager.manager.readConfigSU()) {
                ConfigManager.manager.saveConfigSU()
            }
            lastError = ""
            true
        } catch (e: Throwable) {
            lastError = e.message ?: "读取配置失败"
            false
        }
    }

    fun saveGlobalSettingsFromMemory(): Boolean {
        GlobalVars.globalSettings = GlobalSettings.ensureInitialized(GlobalVars.globalSettings)
        return try {
            val global = GlobalVars.globalSettings ?: GlobalSettings()
            val ok = ConfigManager.manager.applyGlobalSettingsJsonSU(gson.toJson(global))
            lastError = if (ok) "" else "更新全局配置失败"
            ok
        } catch (e: Throwable) {
            lastError = e.message ?: "更新全局配置失败"
            false
        }
    }

    fun saveApplicationSettingsFromMemory(): Boolean {
        GlobalVars.applicationSettings = ApplicationSettings.ensureInitialized(GlobalVars.applicationSettings)
        return try {
            val app = GlobalVars.applicationSettings ?: ApplicationSettings()
            val ok = ConfigManager.manager.applyApplicationSettingsJsonSU(gson.toJson(app))
            lastError = if (ok) "" else "更新应用配置失败"
            ok
        } catch (e: Throwable) {
            lastError = e.message ?: "更新应用配置失败"
            false
        }
    }

    fun getGlobalSettingsJsonOrNull(): String? {
        return try {
            ConfigManager.manager.dumpGlobalSettingsJson()
        } catch (e: Throwable) {
            lastError = e.message ?: "读取全局配置失败"
            null
        }
    }

    fun getApplicationSettingsJsonOrNull(): String? {
        return try {
            ConfigManager.manager.dumpApplicationSettingsJson()
        } catch (e: Throwable) {
            lastError = e.message ?: "读取应用配置失败"
            null
        }
    }

    fun applySettingsJson(globalJson: String, applicationJson: String): Boolean {
        return try {
            val globalOk = ConfigManager.manager.applyGlobalSettingsJsonSU(globalJson)
            val applicationOk = ConfigManager.manager.applyApplicationSettingsJsonSU(applicationJson)
            val ok = globalOk && applicationOk
            lastError = if (ok) "" else "恢复配置失败"
            ok
        } catch (e: Throwable) {
            lastError = e.message ?: "恢复配置失败"
            false
        }
    }

    fun getLastErrorOrDefault(defaultMessage: String): String {
        return lastError.ifBlank { defaultMessage }
    }
}
