package nep.timeline.cirno.ui.page.material

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import top.yukonga.miuix.kmp.blur.isRenderEffectSupported

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
    val uiStyleItems = listOf(stringResource(R.string.ui_style_miuix), stringResource(R.string.ui_style_material))
    val themeItems = listOf(
        stringResource(R.string.theme_follow_system),
        stringResource(R.string.theme_light),
        stringResource(R.string.theme_dark),
        stringResource(R.string.theme_monet_system),
        stringResource(R.string.theme_monet_light),
        stringResource(R.string.theme_monet_dark),
    )
    val navItems = listOf(stringResource(R.string.normal), stringResource(R.string.floating), stringResource(R.string.apple_floating))
    val uiStyleIndex = remember { mutableIntStateOf(globalSettings.uiStyle.coerceIn(UI_STYLE_MIUIX, UI_STYLE_MATERIAL)) }
    val navIndex = remember { mutableIntStateOf(globalSettings.navigationStyle.coerceIn(0, 2)) }
    val themeIndex = remember { mutableIntStateOf(globalSettings.colorMode.coerceIn(0, 5)) }
    val blurEnabled = remember { mutableIntStateOf(if (globalSettings.blurUI) 1 else 0) }
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
        uiStyleIndex.intValue = globalSettings.uiStyle.coerceIn(UI_STYLE_MIUIX, UI_STYLE_MATERIAL)
        navIndex.intValue = globalSettings.navigationStyle.coerceIn(0, 2)
        themeIndex.intValue = globalSettings.colorMode.coerceIn(0, 5)
        blurEnabled.intValue = if (globalSettings.blurUI) 1 else 0
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 20.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.settings),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "v${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (active) {
                item {
                    MaterialSettingsSection(title = stringResource(R.string.settings_freeze_group)) {
                        MaterialSliderItem(
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
                        MaterialDropdownItem(stringResource(R.string.ui_style), uiStyleItems, uiStyleIndex.intValue) {
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
                        MaterialDropdownItem(stringResource(R.string.navigation_style), navItems, navIndex.intValue) {
                            val previous = globalSettings.navigationStyle
                            navIndex.intValue = it
                            globalSettings.navigationStyle = it
                            updateAppState { state -> state.copy(navigationStyle = it) }
                            saveGlobalSettingsAsync("导航样式更新失败") {
                                globalSettings.navigationStyle = previous
                                navIndex.intValue = previous.coerceIn(0, 2)
                                updateAppState { state -> state.copy(navigationStyle = previous) }
                            }
                        }
                        MaterialDropdownItem(stringResource(R.string.theme_mode), themeItems, themeIndex.intValue) {
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
                        if (isRenderEffectSupported()) {
                            MaterialSwitchItem(stringResource(R.string.blur_ui), stringResource(R.string.blur_ui_desc), blurEnabled.intValue == 1) {
                                val previous = globalSettings.blurUI
                                blurEnabled.intValue = if (it) 1 else 0
                                globalSettings.blurUI = it
                                updateAppState { state -> state.copy(blur = it) }
                                saveGlobalSettingsAsync("模糊效果更新失败") {
                                    globalSettings.blurUI = previous
                                    blurEnabled.intValue = if (previous) 1 else 0
                                    updateAppState { state -> state.copy(blur = previous) }
                                }
                            }
                        }
                    }
                }
            }

            item {
                MaterialSettingsSection(title = stringResource(R.string.settings_log_group)) {
                    MaterialDropdownItem(stringResource(R.string.log_print), outputItems, outputIndex.intValue) {
                        val previous = globalSettings.logOutputMode
                        outputIndex.intValue = it
                        globalSettings.logOutputMode = if (it == 0) GlobalSettings.LOG_OUTPUT_FRAMEWORK else GlobalSettings.LOG_OUTPUT_FILE
                        saveGlobalSettingsAsync("日志输出更新失败") {
                            globalSettings.logOutputMode = previous
                            outputIndex.intValue = if (previous == GlobalSettings.LOG_OUTPUT_FRAMEWORK) 0 else 1
                        }
                    }
                    MaterialDropdownItem(stringResource(R.string.log_level), levelItems, levelIndex.intValue) {
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
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.backup_config)) },
                            supportingContent = { Text(stringResource(R.string.backup_config_desc)) },
                            modifier = Modifier.fillMaxWidth().clickable { backupLauncher.launch("cirno-config-backup.zip") },
                        )
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.restore_config)) },
                            supportingContent = { Text(stringResource(R.string.restore_config_desc)) },
                            modifier = Modifier.fillMaxWidth().clickable { restoreLauncher.launch(arrayOf("application/zip", "application/octet-stream")) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MaterialSettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            Column(modifier = Modifier.fillMaxWidth(), content = content)
        }
    }
}

@Composable
private fun MaterialSwitchItem(title: String, summary: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(summary) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) },
    )
}

@Composable
private fun MaterialSliderItem(
    title: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    onValueFinished: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(valueText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = valueRange, steps = steps, onValueChangeFinished = onValueFinished)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MaterialDropdownItem(title: String, items: List<String>, selectedIndex: Int, onSelectedIndexChange: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = items[selectedIndex],
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            label = { Text(title) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.forEachIndexed { index, item ->
                DropdownMenuItem(
                    text = { Text(item) },
                    onClick = {
                        expanded = false
                        onSelectedIndexChange(index)
                    },
                )
            }
        }
    }
}

private fun materialFormatSpeedThreshold(bytesPerSec: Int): String {
    if (bytesPerSec < 1048576) return "${bytesPerSec / 1024} KB/s"
    return String.format("%.2f MB/s", bytesPerSec / 1048576.0)
}
