package nep.timeline.cirno.ui.utils

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import nep.timeline.cirno.GlobalVars
import nep.timeline.cirno.binder.BinderService
import nep.timeline.cirno.configs.settings.ApplicationSettings
import nep.timeline.cirno.configs.settings.GlobalSettings
import nep.timeline.cirno.provide.ConfigBinder
import java.util.LinkedHashSet

object ConfigBinderRepository {
    private val gson = Gson()

    data class InfoBinderSnapshot(
        val binderAvailable: Boolean,
        val hasError: Boolean = false,
        val androidReady: Boolean = false,
        val systemUiReady: Boolean = false,
        val moduleVersion: String? = null
    )

    fun loadIntoMemory(): Boolean {
        BinderService.register(AppContext.context)
        val config = ConfigBinder.getInstance() ?: return false
        return try {
            val globalJson = config.getGlobalSettingsJson()
            val appJson = config.getApplicationSettingsJson()
            val global = gson.fromJson(globalJson, GlobalSettings::class.java) ?: GlobalSettings()
            val app = ApplicationSettings.ensureInitialized(gson.fromJson(appJson, ApplicationSettings::class.java))
            GlobalVars.globalSettings = global
            GlobalVars.applicationSettings = app
            true
        } catch (_: JsonSyntaxException) {
            false
        } catch (_: Throwable) {
            false
        }
    }

    fun saveGlobalSettingsFromMemory(): Boolean {
        val config = ConfigBinder.getInstance() ?: return false
        return try {
            val global = GlobalVars.globalSettings ?: GlobalSettings()
            config.setGlobalSettingsJson(gson.toJson(global))
        } catch (_: Throwable) {
            false
        }
    }

    fun saveApplicationSettingsFromMemory(): Boolean {
        val config = ConfigBinder.getInstance() ?: return false
        return try {
            val app = ApplicationSettings.ensureInitialized(GlobalVars.applicationSettings)
            GlobalVars.applicationSettings = app
            config.setApplicationSettingsJson(gson.toJson(app))
        } catch (_: Throwable) {
            false
        }
    }

    fun getLastErrorOrDefault(defaultMessage: String): String {
        val config = ConfigBinder.getInstance() ?: return defaultMessage
        return try {
            val msg = config.getLastError()
            if (msg.isNullOrBlank()) defaultMessage else msg
        } catch (_: Throwable) {
            defaultMessage
        }
    }

    fun hasErrorSignal(): Boolean {
        val config = ConfigBinder.getInstance() ?: return false
        return try {
            config.getSignal("error") == "1"
        } catch (_: Throwable) {
            false
        }
    }

    fun isAndroidHookReady(): Boolean {
        val config = ConfigBinder.getInstance() ?: return false
        return try {
            config.getSignal("android_hook_ready") == "1"
        } catch (_: Throwable) {
            false
        }
    }

    fun isSystemUIHookReady(): Boolean {
        val config = ConfigBinder.getInstance() ?: return false
        return try {
            config.getSignal("systemui_hook_ready") == "1"
        } catch (_: Throwable) {
            false
        }
    }

    fun getManagedAppKeySet(): Set<String> {
        val config = ConfigBinder.getInstance() ?: return emptySet()
        return try {
            LinkedHashSet(config.getManagedAppKeys() ?: emptyList())
        } catch (_: Throwable) {
            emptySet()
        }
    }

    fun getGlobalSettingsJsonOrNull(): String? {
        val config = ConfigBinder.getInstance() ?: return null
        return try {
            config.getGlobalSettingsJson()
        } catch (_: Throwable) {
            null
        }
    }

    fun getApplicationSettingsJsonOrNull(): String? {
        val config = ConfigBinder.getInstance() ?: return null
        return try {
            config.getApplicationSettingsJson()
        } catch (_: Throwable) {
            null
        }
    }

    fun applySettingsJson(globalJson: String, applicationJson: String): Boolean {
        val config = ConfigBinder.getInstance() ?: return false
        return try {
            val globalOk = config.setGlobalSettingsJson(globalJson)
            val applicationOk = config.setApplicationSettingsJson(applicationJson)
            globalOk && applicationOk
        } catch (_: Throwable) {
            false
        }
    }

    fun getModuleVersion(): String? {
        val config = ConfigBinder.getInstance() ?: return null
        return try {
            val version = config.moduleVersion
            if (version.isNullOrBlank()) null else version
        } catch (_: Throwable) {
            null
        }
    }

    fun isReKernelAvailable(): Boolean {
        val config = ConfigBinder.getInstance() ?: return false
        return try {
            config.isReKernelAvailable
        } catch (_: Throwable) {
            false
        }
    }

    fun loadInfoBinderSnapshot(): InfoBinderSnapshot {
        BinderService.register(AppContext.context)
        val config = ConfigBinder.getInstance() ?: return InfoBinderSnapshot(binderAvailable = false)
        return try {
            val version = config.moduleVersion
            InfoBinderSnapshot(
                binderAvailable = true,
                hasError = config.getSignal("error") == "1",
                androidReady = config.getSignal("android_hook_ready") == "1",
                systemUiReady = config.getSignal("systemui_hook_ready") == "1",
                moduleVersion = if (version.isNullOrBlank()) null else version
            )
        } catch (_: Throwable) {
            InfoBinderSnapshot(binderAvailable = false)
        }
    }

    fun getLogContent(): String? {
        val config = ConfigBinder.getInstance() ?: return null
        return try {
            config.logContent
        } catch (_: Throwable) {
            null
        }
    }

    fun getLogContentPage(startLine: Int, lineCount: Int): List<String> {
        val config = ConfigBinder.getInstance() ?: return emptyList()
        return try {
            config.getLogContentPage(startLine, lineCount)
                .lines()
                .filter { it.isNotBlank() }
        } catch (_: Throwable) {
            emptyList()
        }
    }
}
