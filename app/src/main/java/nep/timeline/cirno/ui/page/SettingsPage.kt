@file:OptIn(ExperimentalScrollBarApi::class)

package nep.timeline.cirno.ui.page

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import nep.timeline.cirno.GlobalVars
import nep.timeline.cirno.R
import nep.timeline.cirno.configs.settings.GlobalSettings
import nep.timeline.cirno.ui.app.LocalIsWideScreen
import nep.timeline.cirno.ui.app.LocalUpdateAppState
import nep.timeline.cirno.ui.utils.AdaptiveTopAppBar
import nep.timeline.cirno.ui.utils.AppContext
import nep.timeline.cirno.ui.utils.BlurredBar
import nep.timeline.cirno.ui.utils.ConfigBackupZipUtils
import nep.timeline.cirno.ui.utils.ConfigBinderRepository
import nep.timeline.cirno.ui.utils.WindowUtils
import nep.timeline.cirno.ui.utils.pageContentPadding
import nep.timeline.cirno.ui.utils.pageScrollModifiers
import nep.timeline.cirno.ui.utils.rememberBlurBackdrop
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.VerticalScrollBar
import top.yukonga.miuix.kmp.basic.rememberScrollBarAdapter
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.isRenderEffectSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.interfaces.ExperimentalScrollBarApi
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsPage(
    active: Boolean,
    padding: PaddingValues,
    scrollEndHaptic: Boolean
) {
    val isWideScreen = LocalIsWideScreen.current
    val backdrop = rememberBlurBackdrop()
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.surface
    val scrollBehavior = MiuixScrollBehavior()

    Scaffold(
        topBar = {
            BlurredBar(backdrop, blurActive, scrollBehavior) {
                AdaptiveTopAppBar(
                    title = stringResource(R.string.settings),
                    isWideScreen = isWideScreen,
                    scrollBehavior = scrollBehavior,
                    color = barColor,
                )
            }
        }
    ) { innerPadding ->
        SettingsContent(
            active = active,
            padding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding(),
            ),
            topAppBarScrollBehavior = scrollBehavior,
            backdrop = backdrop,
            scrollEndHaptic = scrollEndHaptic,
        )
    }
}

@Composable
private fun SettingsContent(
    active: Boolean,
    padding: PaddingValues,
    topAppBarScrollBehavior: top.yukonga.miuix.kmp.basic.ScrollBehavior,
    backdrop: LayerBackdrop?,
    scrollEndHaptic: Boolean,
) {
    val context = LocalContext.current
    val isWideScreen = LocalIsWideScreen.current
    val updateAppState = LocalUpdateAppState.current
    val lazyListState = rememberLazyListState()
    val contentPadding = pageContentPadding(padding, padding, isWideScreen)
    val scope = rememberCoroutineScope()
    var globalSettings = GlobalVars.globalSettings ?: GlobalSettings().also { GlobalVars.globalSettings = it }
    val backupSuccessText = stringResource(R.string.backup_success)
    val backupFailedText = stringResource(R.string.backup_failed)
    val restoreSuccessText = stringResource(R.string.restore_success)
    val restoreSuccessReloadFailedText = stringResource(R.string.restore_success_reload_failed)
    val restoreFailedApplyText = stringResource(R.string.restore_failed_apply)
    val restoreFailedOpenText = stringResource(R.string.restore_failed_open)
    val restoreFailedStructureText = stringResource(R.string.restore_failed_structure)
    val restoreFailedRequiredFilesText = stringResource(R.string.restore_failed_required_files)
    val restoreFailedJsonText = stringResource(R.string.restore_failed_json)
    val restoreFailedIoText = stringResource(R.string.restore_failed_io)
    val restoreFailedUnknownText = stringResource(R.string.restore_failed_unknown)
    val backupFileName = remember {
        val time = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(Date())
        "cirno-config-backup-$time.zip"
    }
    val freezeDelay = remember { mutableFloatStateOf(globalSettings.freezeDelay.toFloat()) }
    val wakeFreezeDelay = remember { mutableFloatStateOf(globalSettings.wakeFreezeDelay.toFloat()) }
    val networkSpeedThreshold = remember { mutableFloatStateOf(globalSettings.networkSpeedThreshold.toFloat()) }
    val navItems = listOf(
        stringResource(R.string.normal),
        stringResource(R.string.floating),
        stringResource(R.string.apple_floating),
    )
    val themeItems = listOf(
        stringResource(R.string.theme_follow_system),
        stringResource(R.string.theme_light),
        stringResource(R.string.theme_dark),
        stringResource(R.string.theme_monet_system),
        stringResource(R.string.theme_monet_light),
        stringResource(R.string.theme_monet_dark),
    )
    val navIndex = remember { mutableIntStateOf(globalSettings.navigationStyle.coerceIn(0, 2)) }
    val themeIndex = remember { mutableIntStateOf(globalSettings.colorMode.coerceIn(0, 5)) }
    val blurEnabled = remember { mutableIntStateOf(if (globalSettings.blurUI) 1 else 0) }
    val outputItems = listOf(
        stringResource(R.string.log_xposed),
        stringResource(R.string.log_file),
    )
    val outputIndex = remember {
        mutableIntStateOf(
            if (GlobalVars.globalSettings.logOutputMode == GlobalSettings.LOG_OUTPUT_FRAMEWORK) 0 else 1
        )
    }
    val levelItems = listOf(
        stringResource(R.string.log_close),
        stringResource(R.string.log_info),
        stringResource(R.string.log_debug),
    )
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
                if (ConfigBinderRepository.saveGlobalSettingsFromMemory()) {
                    null
                } else {
                    ConfigBinderRepository.getLastErrorOrDefault(defaultError)
                }
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

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        if (uri == null) {
            return@rememberLauncherForActivityResult
        }
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

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) {
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            val (message, restored) = withContext(Dispatchers.IO) {
                try {
                    val restored = ConfigBackupZipUtils.readAndValidateBackupZip(context.contentResolver, uri)
                    val applied = ConfigBinderRepository.applySettingsJson(restored.globalJson, restored.applicationJson)
                    if (!applied) {
                        return@withContext ConfigBinderRepository.getLastErrorOrDefault(restoreFailedApplyText) to false
                    }
                    if (!ConfigBinderRepository.loadIntoMemory()) {
                        return@withContext restoreSuccessReloadFailedText to false
                    }
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
                        navigationStyle = globalSettings.navigationStyle,
                        colorMode = globalSettings.colorMode,
                        blur = globalSettings.blurUI,
                    )
                }
            }
            AppContext.showToast(message)
        }
    }

    Box(modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.pageScrollModifiers(scrollEndHaptic, true, topAppBarScrollBehavior),
            contentPadding = contentPadding,
        ) {
            if (active) {
                item {
                    SmallTitle(text = stringResource(R.string.settings_freeze_group))
                    Card(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = stringResource(R.string.interval_freeze_delay) + " | " + freezeDelay.floatValue.toInt() + " s",
                            modifier = Modifier.padding(17.dp),
                        )
                        Slider(
                            value = freezeDelay.floatValue,
                            onValueChange = {
                                freezeDelay.floatValue = it
                            },
                            onValueChangeFinished = {
                                val previous = globalSettings.freezeDelay
                                globalSettings.freezeDelay = freezeDelay.floatValue.toInt().coerceAtLeast(1)
                                saveGlobalSettingsAsync("冻结延迟更新失败") {
                                    globalSettings.freezeDelay = previous
                                    freezeDelay.floatValue = previous.toFloat()
                                }
                            },
                            valueRange = 1f..30f,
                            steps = 28,
                            modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp),
                        )
                        Text(
                            text = stringResource(R.string.wake_freeze_delay) + " | " + wakeFreezeDelay.floatValue.toInt() + " s",
                            modifier = Modifier.padding(17.dp),
                        )
                        Slider(
                            value = wakeFreezeDelay.floatValue,
                            onValueChange = {
                                wakeFreezeDelay.floatValue = it
                            },
                            onValueChangeFinished = {
                                val previous = globalSettings.wakeFreezeDelay
                                globalSettings.wakeFreezeDelay = wakeFreezeDelay.floatValue.toInt().coerceIn(1,120)
                                saveGlobalSettingsAsync("唤醒冻结延迟更新失败") {
                                    globalSettings.wakeFreezeDelay = previous
                                    wakeFreezeDelay.floatValue = previous.toFloat()
                                }
                            },
                            valueRange = 1f..120f,
                            steps = 118,
                            modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp),
                        )
                        Text(
                            text = stringResource(R.string.network_speed_threshold) + " | " + formatSpeedThreshold(networkSpeedThreshold.floatValue.toInt()),
                            modifier = Modifier.padding(17.dp),
                        )
                        Slider(
                            value = networkSpeedThreshold.floatValue,
                            onValueChange = {
                                networkSpeedThreshold.floatValue = it
                            },
                            onValueChangeFinished = {
                                val previous = globalSettings.networkSpeedThreshold
                                globalSettings.networkSpeedThreshold = networkSpeedThreshold.floatValue.toInt().coerceIn(102400, 2097152)
                                saveGlobalSettingsAsync("网速识别阈值更新失败") {
                                    globalSettings.networkSpeedThreshold = previous
                                    networkSpeedThreshold.floatValue = previous.toFloat()
                                }
                            },
                            valueRange = 102400f..2097152f,
                            steps = 99,
                            modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp),
                        )
                    }
                }

                item {
                    SmallTitle(text = stringResource(R.string.settings_ui_group))
                    Card(modifier = Modifier.padding(12.dp)) {
                        OverlayDropdownPreference(
                            title = stringResource(R.string.navigation_style),
                            items = navItems,
                            selectedIndex = navIndex.intValue,
                            onSelectedIndexChange = {
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
                        )

                        OverlayDropdownPreference(
                            title = stringResource(R.string.theme_mode),
                            items = themeItems,
                            selectedIndex = themeIndex.intValue,
                            onSelectedIndexChange = {
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
                        )

                        if (isRenderEffectSupported()) {
                            SwitchPreference(
                                title = stringResource(R.string.blur_ui),
                                summary = stringResource(R.string.blur_ui_desc),
                                checked = blurEnabled.intValue == 1,
                                onCheckedChange = {
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
                            )
                        }
                    }
                }

                item {
                    SmallTitle(text = stringResource(R.string.settings_log_group))
                    Card(modifier = Modifier.padding(12.dp)) {
                        OverlayDropdownPreference(
                            title = stringResource(R.string.log_print),
                            items = outputItems,
                            selectedIndex = outputIndex.intValue,
                            onSelectedIndexChange = {
                                val previous = globalSettings.logOutputMode
                                outputIndex.intValue = it
                                globalSettings.logOutputMode = if (it == 0) GlobalSettings.LOG_OUTPUT_FRAMEWORK else GlobalSettings.LOG_OUTPUT_FILE
                                saveGlobalSettingsAsync("日志输出更新失败") {
                                    globalSettings.logOutputMode = previous
                                    outputIndex.intValue = if (previous == GlobalSettings.LOG_OUTPUT_FRAMEWORK) 0 else 1
                                }
                            }
                        )
                        OverlayDropdownPreference(
                            title = stringResource(R.string.log_level),
                            items = levelItems,
                            selectedIndex = levelIndex.intValue,
                            onSelectedIndexChange = {
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
                        )
                    }
                }

                item {
                    SmallTitle(text = stringResource(R.string.settings_backup_group))
                    Card(modifier = Modifier.padding(12.dp)) {
                        ArrowPreference(
                            title = stringResource(R.string.backup_config),
                            summary = stringResource(R.string.backup_config_desc),
                            onClick = {
                                backupLauncher.launch(backupFileName)
                            }
                        )

                        ArrowPreference(
                            title = stringResource(R.string.restore_config),
                            summary = stringResource(R.string.restore_config_desc),
                            onClick = {
                                restoreLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
                            }
                        )
                    }
                }
            }
        }

        VerticalScrollBar(
            adapter = rememberScrollBarAdapter(lazyListState),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            trackPadding = contentPadding,
        )
    }
}

private fun formatSpeedThreshold(bytesPerSec: Int): String {
    if (bytesPerSec < 1048576) return "${bytesPerSec / 1024} KB/s"
    return String.format("%.2f MB/s", bytesPerSec / 1048576.0)
}
