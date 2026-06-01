package nep.timeline.cirno.ui.page.material

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nep.timeline.cirno.BuildConfig
import nep.timeline.cirno.GlobalVars
import nep.timeline.cirno.R
import nep.timeline.cirno.configs.settings.GlobalSettings
import nep.timeline.cirno.ui.app.LocalUpdateAppState
import nep.timeline.cirno.ui.app.UI_STYLE_MATERIAL
import nep.timeline.cirno.ui.app.UI_STYLE_MIUIX
import nep.timeline.cirno.ui.utils.AppContext
import nep.timeline.cirno.ui.utils.ConfigBackupZipUtils
import nep.timeline.cirno.ui.utils.ConfigBinderRepository
import nep.timeline.cirno.ui.utils.WindowUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialSettingsPage(
    active: Boolean,
    padding: PaddingValues,
) {
    val context = LocalContext.current
    val updateAppState = LocalUpdateAppState.current
    val scope = rememberCoroutineScope()
    var globalSettings = GlobalVars.globalSettings ?: GlobalSettings().also { GlobalVars.globalSettings = it }

    val backupFailedText = stringResource(R.string.backup_failed)
    val backupSuccessText = stringResource(R.string.backup_success)
    val restoreSuccessText = stringResource(R.string.restore_success)
    val restoreSuccessReloadFailedText = stringResource(R.string.restore_success_reload_failed)
    val restoreFailedApplyText = stringResource(R.string.restore_failed_apply)
    val restoreFailedOpenText = stringResource(R.string.restore_failed_open)
    val restoreFailedStructureText = stringResource(R.string.restore_failed_structure)
    val restoreFailedRequiredFilesText = stringResource(R.string.restore_failed_required_files)
    val restoreFailedJsonText = stringResource(R.string.restore_failed_json)
    val restoreFailedIoText = stringResource(R.string.restore_failed_io)
    val restoreFailedUnknownText = stringResource(R.string.restore_failed_unknown)

    val freezeDelay = remember { mutableFloatStateOf(globalSettings.freezeDelay.toFloat()) }
    val wakeFreezeDelay = remember { mutableFloatStateOf(globalSettings.wakeFreezeDelay.toFloat()) }
    val networkSpeedThreshold = remember { mutableFloatStateOf(globalSettings.networkSpeedThreshold.toFloat()) }
    val freezerModeItems = listOf(stringResource(R.string.freezer_mode_uid), stringResource(R.string.freezer_mode_frozen))
    val uiStyleItems = listOf(stringResource(R.string.ui_style_miuix), stringResource(R.string.ui_style_material))
    val themeItems = listOf(
        stringResource(R.string.theme_follow_system),
        stringResource(R.string.theme_light),
        stringResource(R.string.theme_dark),
        stringResource(R.string.theme_monet_system),
        stringResource(R.string.theme_monet_light),
        stringResource(R.string.theme_monet_dark),
    )
    val uiStyleIndex = remember { mutableIntStateOf(globalSettings.uiStyle.coerceIn(UI_STYLE_MIUIX, UI_STYLE_MATERIAL)) }
    val freezerModeIndex = remember { mutableIntStateOf(if (globalSettings.freezerMode == GlobalSettings.FREEZER_MODE_FROZEN) 1 else 0) }
    val themeIndex = remember { mutableIntStateOf(globalSettings.colorMode.coerceIn(0, 5)) }
    val outputItems = listOf(stringResource(R.string.log_xposed), stringResource(R.string.log_file))
    val outputIndex = remember { mutableIntStateOf(if (GlobalVars.globalSettings.logOutputMode == GlobalSettings.LOG_OUTPUT_FRAMEWORK) 0 else 1) }
    val levelItems = listOf(stringResource(R.string.log_close), stringResource(R.string.log_info), stringResource(R.string.log_debug))
    val levelIndex = remember {
        mutableIntStateOf(
            when (GlobalVars.globalSettings.logLevel) {
                GlobalSettings.LOG_LEVEL_NONE -> 0
                GlobalSettings.LOG_LEVEL_DEBUG -> 2
                else -> 1
            }
        )
    }
    fun saveGlobalSettingsAsync(defaultError: String, onFailed: () -> Unit) {
        scope.launch {
            val error = withContext(Dispatchers.IO) {
                if (ConfigBinderRepository.saveGlobalSettingsFromMemory()) null else ConfigBinderRepository.getLastErrorOrDefault(defaultError)
            }
            if (error != null) {
                onFailed()
                WindowUtils.showToast(error)
            }
        }
    }

    fun syncLocalStateFromSettings() {
        freezeDelay.floatValue = globalSettings.freezeDelay.toFloat()
        wakeFreezeDelay.floatValue = globalSettings.wakeFreezeDelay.toFloat()
        networkSpeedThreshold.floatValue = globalSettings.networkSpeedThreshold.toFloat()
        freezerModeIndex.intValue = if (globalSettings.freezerMode == GlobalSettings.FREEZER_MODE_FROZEN) 1 else 0
        uiStyleIndex.intValue = globalSettings.uiStyle.coerceIn(UI_STYLE_MIUIX, UI_STYLE_MATERIAL)
        themeIndex.intValue = globalSettings.colorMode.coerceIn(0, 5)
        outputIndex.intValue = if (globalSettings.logOutputMode == GlobalSettings.LOG_OUTPUT_FRAMEWORK) 0 else 1
        levelIndex.intValue = when (globalSettings.logLevel) {
            GlobalSettings.LOG_LEVEL_NONE -> 0
            GlobalSettings.LOG_LEVEL_DEBUG -> 2
            else -> 1
        }
    }

    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val message = withContext(Dispatchers.IO) {
                val globalJson = ConfigBinderRepository.getGlobalSettingsJsonOrNull()
                val applicationJson = ConfigBinderRepository.getApplicationSettingsJsonOrNull()
                if (globalJson == null || applicationJson == null) {
                    return@withContext ConfigBinderRepository.getLastErrorOrDefault(backupFailedText)
                }
                try {
                    ConfigBackupZipUtils.writeBackupZip(context.contentResolver, uri, globalJson, applicationJson)
                    backupSuccessText
                } catch (_: Throwable) {
                    backupFailedText
                }
            }
            AppContext.showToast(message)
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val (message, restored) = withContext(Dispatchers.IO) {
                try {
                    val restored = ConfigBackupZipUtils.readAndValidateBackupZip(context.contentResolver, uri)
                    val applied = ConfigBinderRepository.applySettingsJson(restored.globalJson, restored.applicationJson)
                    if (!applied) return@withContext ConfigBinderRepository.getLastErrorOrDefault(restoreFailedApplyText) to false
                    if (!ConfigBinderRepository.loadIntoMemory()) return@withContext restoreSuccessReloadFailedText to false
                    restoreSuccessText to true
                } catch (e: ConfigBackupZipUtils.RestoreException) {
                    when (e.error) {
                        ConfigBackupZipUtils.RestoreError.OPEN_INPUT_FAILED -> restoreFailedOpenText
                        ConfigBackupZipUtils.RestoreError.INVALID_ZIP_STRUCTURE -> restoreFailedStructureText
                        ConfigBackupZipUtils.RestoreError.MISSING_REQUIRED_FILES -> restoreFailedRequiredFilesText
                        ConfigBackupZipUtils.RestoreError.INVALID_JSON -> restoreFailedJsonText
                        ConfigBackupZipUtils.RestoreError.IO_ERROR -> restoreFailedIoText
                    } to false
                } catch (_: Throwable) {
                    restoreFailedUnknownText to false
                }
            }
            if (restored) {
                globalSettings = GlobalVars.globalSettings ?: globalSettings
                syncLocalStateFromSettings()
                updateAppState { state ->
                    state.copy(
                        uiStyle = globalSettings.uiStyle,
                        navigationStyle = globalSettings.navigationStyle,
                        colorMode = globalSettings.colorMode,
                        blur = globalSettings.blurUI,
                    )
                }
            }
            AppContext.showToast(message)
        }
    }

    MaterialPageScaffold(
        title = stringResource(R.string.settings),
        padding = padding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "v${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (active) {
            item {
                MaterialSettingsSection(title = stringResource(R.string.settings_freeze_group)) {
                    MaterialDropdownItem(Icons.Outlined.Update, stringResource(R.string.freezer_mode), freezerModeItems, freezerModeIndex.intValue) {
                        val previousMode = globalSettings.freezerMode
                        val previousIndex = freezerModeIndex.intValue
                        scope.launch {
                            val (uidAvailable, frozenAvailable) = withContext(Dispatchers.IO) {
                                ConfigBinderRepository.isUidFreezerAvailable() to ConfigBinderRepository.isFrozenFreezerAvailable()
                            }
                            when {
                                uidAvailable -> {
                                    freezerModeIndex.intValue = 0
                                    globalSettings.freezerMode = GlobalSettings.FREEZER_MODE_UID
                                }
                                frozenAvailable -> {
                                    freezerModeIndex.intValue = 1
                                    globalSettings.freezerMode = GlobalSettings.FREEZER_MODE_FROZEN
                                }
                                else -> {
                                    WindowUtils.showToast(context.getString(R.string.freezer_v2_unavailable))
                                    return@launch
                                }
                            }
                            saveGlobalSettingsAsync("冻结模式更新失败") {
                                globalSettings.freezerMode = previousMode
                                freezerModeIndex.intValue = previousIndex
                            }
                        }
                    }
                    MaterialSliderItem(
                        icon = Icons.Outlined.Timer,
                        title = stringResource(R.string.interval_freeze_delay),
                        valueText = "${freezeDelay.floatValue.toInt()} s",
                        value = freezeDelay.floatValue,
                        valueRange = 1f..30f,
                        steps = 28,
                        onValueChange = { freezeDelay.floatValue = it },
                        onValueFinished = {
                            val previous = globalSettings.freezeDelay
                            globalSettings.freezeDelay = freezeDelay.floatValue.toInt().coerceAtLeast(1)
                            saveGlobalSettingsAsync("冻结延迟更新失败") {
                                globalSettings.freezeDelay = previous
                                freezeDelay.floatValue = previous.toFloat()
                            }
                        },
                    )
                    MaterialSliderItem(
                        icon = Icons.Outlined.Update,
                        title = stringResource(R.string.wake_freeze_delay),
                        valueText = "${wakeFreezeDelay.floatValue.toInt()} s",
                        value = wakeFreezeDelay.floatValue,
                        valueRange = 1f..120f,
                        steps = 118,
                        onValueChange = { wakeFreezeDelay.floatValue = it },
                        onValueFinished = {
                            val previous = globalSettings.wakeFreezeDelay
                            globalSettings.wakeFreezeDelay = wakeFreezeDelay.floatValue.toInt().coerceIn(1, 120)
                            saveGlobalSettingsAsync("唤醒冻结延迟更新失败") {
                                globalSettings.wakeFreezeDelay = previous
                                wakeFreezeDelay.floatValue = previous.toFloat()
                            }
                        },
                    )
                    MaterialSliderItem(
                        icon = Icons.Outlined.Speed,
                        title = stringResource(R.string.network_speed_threshold),
                        valueText = materialFormatSpeedThreshold(networkSpeedThreshold.floatValue.toInt()),
                        value = networkSpeedThreshold.floatValue,
                        valueRange = 102400f..2097152f,
                        steps = 99,
                        onValueChange = { networkSpeedThreshold.floatValue = it },
                        onValueFinished = {
                            val previous = globalSettings.networkSpeedThreshold
                            globalSettings.networkSpeedThreshold = networkSpeedThreshold.floatValue.toInt().coerceIn(102400, 2097152)
                            saveGlobalSettingsAsync("网速识别阈值更新失败") {
                                globalSettings.networkSpeedThreshold = previous
                                networkSpeedThreshold.floatValue = previous.toFloat()
                            }
                        },
                    )
                }
            }
            item {
                MaterialSettingsSection(title = stringResource(R.string.settings_ui_group)) {
                    MaterialDropdownItem(Icons.Outlined.Dashboard, stringResource(R.string.ui_style), uiStyleItems, uiStyleIndex.intValue) {
                        val previous = globalSettings.uiStyle
                        uiStyleIndex.intValue = it
                        globalSettings.uiStyle = it
                        updateAppState { state -> state.copy(uiStyle = it) }
                        saveGlobalSettingsAsync("界面风格更新失败") {
                            globalSettings.uiStyle = previous
                            uiStyleIndex.intValue = previous.coerceIn(UI_STYLE_MIUIX, UI_STYLE_MATERIAL)
                            updateAppState { state -> state.copy(uiStyle = previous) }
                        }
                    }
                    MaterialDropdownItem(Icons.Outlined.Palette, stringResource(R.string.theme_mode), themeItems, themeIndex.intValue) {
                        val previous = globalSettings.colorMode
                        themeIndex.intValue = it
                        globalSettings.colorMode = it
                        updateAppState { state -> state.copy(colorMode = it) }
                        saveGlobalSettingsAsync("主题模式更新失败") {
                            globalSettings.colorMode = previous
                            themeIndex.intValue = previous.coerceIn(0, 5)
                            updateAppState { state -> state.copy(colorMode = previous) }
                        }
                    }
                }
            }
        }

        item {
            MaterialSettingsSection(title = stringResource(R.string.settings_log_group)) {
                MaterialDropdownItem(Icons.Outlined.Article, stringResource(R.string.log_print), outputItems, outputIndex.intValue) {
                    val previous = globalSettings.logOutputMode
                    outputIndex.intValue = it
                    globalSettings.logOutputMode = if (it == 0) GlobalSettings.LOG_OUTPUT_FRAMEWORK else GlobalSettings.LOG_OUTPUT_FILE
                    saveGlobalSettingsAsync("日志输出更新失败") {
                        globalSettings.logOutputMode = previous
                        outputIndex.intValue = if (previous == GlobalSettings.LOG_OUTPUT_FRAMEWORK) 0 else 1
                    }
                }
                MaterialDropdownItem(Icons.Outlined.BugReport, stringResource(R.string.log_level), levelItems, levelIndex.intValue) {
                    val previous = globalSettings.logLevel
                    levelIndex.intValue = it
                    globalSettings.logLevel = when (it) {
                        0 -> GlobalSettings.LOG_LEVEL_NONE
                        2 -> GlobalSettings.LOG_LEVEL_DEBUG
                        else -> GlobalSettings.LOG_LEVEL_INFO
                    }
                    saveGlobalSettingsAsync("日志级别更新失败") {
                        globalSettings.logLevel = previous
                        levelIndex.intValue = when (previous) {
                            GlobalSettings.LOG_LEVEL_NONE -> 0
                            GlobalSettings.LOG_LEVEL_DEBUG -> 2
                            else -> 1
                        }
                    }
                }
            }
        }

        if (active) {
            item {
                MaterialSettingsSection(title = stringResource(R.string.settings_backup_group)) {
                    MaterialActionItem(
                        icon = Icons.Outlined.Backup,
                        title = stringResource(R.string.backup_config),
                        summary = stringResource(R.string.backup_config_desc),
                        onClick = { backupLauncher.launch("cirno-config-backup.zip") },
                    )
                    MaterialActionItem(
                        icon = Icons.Outlined.Restore,
                        title = stringResource(R.string.restore_config),
                        summary = stringResource(R.string.restore_config_desc),
                        onClick = { restoreLauncher.launch(arrayOf("application/zip", "application/octet-stream")) },
                    )
                }
            }
        }
    }
}

private fun materialFormatSpeedThreshold(bytesPerSec: Int): String {
    if (bytesPerSec < 1048576) return "${bytesPerSec / 1024} KB/s"
    return String.format("%.2f MB/s", bytesPerSec / 1048576.0)
}
