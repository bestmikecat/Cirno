package nep.timeline.cirno.ui.utils

import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.io.SuFile
import com.topjohnwu.superuser.io.SuFileInputStream
import nep.timeline.cirno.GlobalVars
import nep.timeline.cirno.binder.BinderService
import nep.timeline.cirno.configs.ConfigManager
import nep.timeline.cirno.configs.settings.ApplicationSettings
import nep.timeline.cirno.configs.settings.GlobalSettings
import nep.timeline.cirno.provide.StatusBinder
import java.nio.charset.StandardCharsets
import java.util.LinkedHashSet
import java.util.regex.Pattern

object ConfigBinderRepository {
    private val gson = com.google.gson.Gson()
    private val userPattern = Pattern.compile("UserInfo\\{(\\d+):")
    private var lastError: String = ""

    data class InfoBinderSnapshot(
        val binderAvailable: Boolean,
        val hasError: Boolean = false,
        val androidReady: Boolean = false,
        val systemUiReady: Boolean = false,
        val hookVersion: String? = null,
    )

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
            if (!ok) {
                lastError = "更新全局配置失败"
            } else {
                lastError = ""
            }
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
            if (!ok) {
                lastError = "更新应用配置失败"
            } else {
                lastError = ""
            }
            ok
        } catch (e: Throwable) {
            lastError = e.message ?: "更新应用配置失败"
            false
        }
    }

    fun getLastErrorOrDefault(defaultMessage: String): String {
        return lastError.ifBlank { defaultMessage }
    }

    fun hasErrorSignal(): Boolean = getSignal("error") == "1"

    fun isAndroidHookReady(): Boolean = getSignal("android_hook_ready") == "1"

    fun isSystemUIHookReady(): Boolean = getSignal("systemui_hook_ready") == "1"

    fun getManagedAppKeySet(): Set<String> {
        val result = LinkedHashSet<String>()
        for (userId in getInstalledUserIdsByRoot()) {
            for (pkg in getInstalledPackagesForUserByRoot(userId)) {
                result.add("$pkg#$userId")
            }
        }
        return result
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
            if (!ok) {
                lastError = "恢复配置失败"
            } else {
                lastError = ""
            }
            ok
        } catch (e: Throwable) {
            lastError = e.message ?: "恢复配置失败"
            false
        }
    }

    fun getModuleVersion(): String? = getHookVersion()

    fun isReKernelAvailable(): Boolean {
        BinderService.register(AppContext.context)
        val status = StatusBinder.getInstance() ?: return false
        return try {
            status.isReKernelAvailable
        } catch (_: Throwable) {
            false
        }
    }

    fun isFrozenFreezerAvailable(): Boolean {
        return try {
            val frozenDir = SuFile("/sys/fs/cgroup/frozen")
            val unfrozenDir = SuFile("/sys/fs/cgroup/unfrozen")
            if (!frozenDir.exists() && !frozenDir.mkdir()) return false
            if (!unfrozenDir.exists() && !unfrozenDir.mkdir()) return false
            SuFile("/sys/fs/cgroup/frozen/cgroup.freeze").exists() &&
                SuFile("/sys/fs/cgroup/unfrozen/cgroup.freeze").exists()
        } catch (_: Throwable) {
            false
        }
    }

    fun isUidFreezerAvailable(): Boolean {
        return try {
            if (SuFile("/sys/fs/cgroup/uid_1000/cgroup.freeze").exists()) {
                SuFile("/sys/fs/cgroup/uid_0/cgroup.freeze").exists()
            } else {
                SuFile("/sys/fs/cgroup/system/uid_0/cgroup.freeze").exists()
            }
        } catch (_: Throwable) {
            false
        }
    }

    fun isAnyFreezerAvailable(): Boolean = isUidFreezerAvailable() || isFrozenFreezerAvailable()

    fun loadInfoBinderSnapshot(): InfoBinderSnapshot {
        BinderService.register(AppContext.context)
        val status = StatusBinder.getInstance() ?: return InfoBinderSnapshot(binderAvailable = false)
        return try {
            val version = status.hookVersion
            InfoBinderSnapshot(
                binderAvailable = true,
                hasError = status.getSignal("error") == "1",
                androidReady = status.getSignal("android_hook_ready") == "1",
                systemUiReady = status.getSignal("systemui_hook_ready") == "1",
                hookVersion = if (version.isNullOrBlank()) null else version,
            )
        } catch (_: Throwable) {
            InfoBinderSnapshot(binderAvailable = false)
        }
    }

    fun getLogContent(): String? {
        return readSuFile(SuFile(GlobalVars.LOG_DIR, "current.log"))
    }

    fun getLogContentPage(startLine: Int, lineCount: Int): List<String> {
        if (lineCount <= 0) return emptyList()
        val content = getLogContent() ?: return emptyList()
        return content
            .lines()
            .drop(startLine.coerceAtLeast(0))
            .take(lineCount)
            .filter { it.isNotBlank() }
    }

    private fun getSignal(key: String): String {
        BinderService.register(AppContext.context)
        val status = StatusBinder.getInstance() ?: return ""
        return try {
            status.getSignal(key).orEmpty()
        } catch (_: Throwable) {
            ""
        }
    }

    private fun getHookVersion(): String? {
        BinderService.register(AppContext.context)
        val status = StatusBinder.getInstance() ?: return null
        return try {
            status.hookVersion.takeIf { !it.isNullOrBlank() }
        } catch (_: Throwable) {
            null
        }
    }

    private fun getInstalledUserIdsByRoot(): List<Int> {
        val userIds = LinkedHashSet<Int>()
        var lines = runRootCommand("pm list users")
        if (lines.isEmpty()) {
            lines = runRootCommand("cmd user list")
        }
        for (line in lines) {
            val matcher = userPattern.matcher(line)
            if (matcher.find()) {
                matcher.group(1)?.toIntOrNull()?.let(userIds::add)
            }
        }
        if (userIds.isEmpty()) {
            userIds.add(0)
        }
        return userIds.toList()
    }

    private fun getInstalledPackagesForUserByRoot(userId: Int): List<String> {
        val packages = LinkedHashSet<String>()
        var lines = runRootCommand("pm list packages --user $userId")
        if (lines.isEmpty()) {
            lines = runRootCommand("cmd package list packages --user $userId")
        }
        for (line in lines) {
            if (!line.startsWith("package:")) continue
            val pkg = line.removePrefix("package:").trim()
            if (pkg.isNotEmpty()) {
                packages.add(pkg)
            }
        }
        return packages.toList()
    }

    private fun runRootCommand(command: String): List<String> {
        return try {
            val result = Shell.cmd(command).exec()
            if (result != null && result.isSuccess) result.out else emptyList()
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun readSuFile(file: SuFile): String? {
        return try {
            if (!file.exists()) return null
            SuFileInputStream.open(file).use { input ->
                String(input.readBytes(), StandardCharsets.UTF_8)
            }
        } catch (e: Throwable) {
            lastError = e.message ?: "读取文件失败"
            null
        }
    }
}
