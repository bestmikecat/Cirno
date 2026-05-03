@file:OptIn(ExperimentalScrollBarApi::class)

package nep.timeline.cirno.ui.page

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
import nep.timeline.cirno.ui.utils.BlurredBar
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
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference

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
    val isWideScreen = LocalIsWideScreen.current
    val updateAppState = LocalUpdateAppState.current
    val lazyListState = rememberLazyListState()
    val contentPadding = pageContentPadding(padding, padding, isWideScreen)
    val globalSettings = GlobalVars.globalSettings ?: GlobalSettings().also { GlobalVars.globalSettings = it }

    Box(modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.pageScrollModifiers(scrollEndHaptic, true, topAppBarScrollBehavior),
            contentPadding = contentPadding,
        ) {
            if (active) {
                item {
                    SmallTitle(text = stringResource(R.string.settings))
                    Card(modifier = Modifier.padding(12.dp)) {
                        val freezeDelay = remember {
                            mutableFloatStateOf(globalSettings.freezeDelay.toFloat())
                        }
                        Text(
                            text = stringResource(R.string.interval_freeze_delay) + " | " + freezeDelay.floatValue.toInt() + " s",
                            modifier = Modifier.padding(17.dp),
                        )
                        Slider(
                            value = freezeDelay.floatValue,
                            onValueChange = {
                                val previous = globalSettings.freezeDelay
                                freezeDelay.floatValue = it
                                globalSettings.freezeDelay = it.toInt().coerceAtLeast(1)
                                if (!ConfigBinderRepository.saveGlobalSettingsFromMemory()) {
                                    globalSettings.freezeDelay = previous
                                    freezeDelay.floatValue = previous.toFloat()
                                    WindowUtils.showToast(ConfigBinderRepository.getLastErrorOrDefault("冻结延迟更新失败"))
                                }
                            },
                            valueRange = 1f..30f,
                            steps = 0,
                            modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp),
                        )

                        val navItems = listOf(
                            stringResource(R.string.normal),
                            stringResource(R.string.floating),
                            stringResource(R.string.apple_floating),
                        )
                        val navIndex = remember {
                            mutableIntStateOf(globalSettings.navigationStyle.coerceIn(0, 2))
                        }
                        OverlayDropdownPreference(
                            title = stringResource(R.string.navigation_style),
                            items = navItems,
                            selectedIndex = navIndex.intValue,
                            onSelectedIndexChange = {
                                val previous = globalSettings.navigationStyle
                                navIndex.intValue = it
                                globalSettings.navigationStyle = it
                                updateAppState { state -> state.copy(navigationStyle = it) }
                                if (!ConfigBinderRepository.saveGlobalSettingsFromMemory()) {
                                    globalSettings.navigationStyle = previous
                                    navIndex.intValue = previous.coerceIn(0, 2)
                                    updateAppState { state -> state.copy(navigationStyle = previous) }
                                    WindowUtils.showToast(ConfigBinderRepository.getLastErrorOrDefault("导航样式更新失败"))
                                }
                            }
                        )

                        if (isRenderEffectSupported()) {
                            val blurEnabled = remember { mutableIntStateOf(if (globalSettings.blurUI) 1 else 0) }
                            SwitchPreference(
                                title = stringResource(R.string.blur_ui),
                                summary = stringResource(R.string.blur_ui_desc),
                                checked = blurEnabled.intValue == 1,
                                onCheckedChange = {
                                    val previous = globalSettings.blurUI
                                    blurEnabled.intValue = if (it) 1 else 0
                                    globalSettings.blurUI = it
                                    updateAppState { state -> state.copy(blur = it) }
                                    if (!ConfigBinderRepository.saveGlobalSettingsFromMemory()) {
                                        globalSettings.blurUI = previous
                                        blurEnabled.intValue = if (previous) 1 else 0
                                        updateAppState { state -> state.copy(blur = previous) }
                                        WindowUtils.showToast(ConfigBinderRepository.getLastErrorOrDefault("模糊效果更新失败"))
                                    }
                                }
                            )
                        }

                        val outputItems = listOf(
                            stringResource(R.string.log_xposed),
                            stringResource(R.string.log_file),
                        )
                        val outputIndex = remember {
                            mutableIntStateOf(
                                if (GlobalVars.globalSettings.logOutputMode == GlobalSettings.LOG_OUTPUT_FRAMEWORK) 0 else 1
                            )
                        }
                        OverlayDropdownPreference(
                            title = stringResource(R.string.log_print),
                            items = outputItems,
                            selectedIndex = outputIndex.intValue,
                            onSelectedIndexChange = {
                                val previous = globalSettings.logOutputMode
                                outputIndex.intValue = it
                                globalSettings.logOutputMode = if (it == 0) GlobalSettings.LOG_OUTPUT_FRAMEWORK else GlobalSettings.LOG_OUTPUT_FILE
                                if (!ConfigBinderRepository.saveGlobalSettingsFromMemory()) {
                                    globalSettings.logOutputMode = previous
                                    outputIndex.intValue = if (previous == GlobalSettings.LOG_OUTPUT_FRAMEWORK) 0 else 1
                                    WindowUtils.showToast(ConfigBinderRepository.getLastErrorOrDefault("日志输出更新失败"))
                                }
                            }
                        )

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
                                if (!ConfigBinderRepository.saveGlobalSettingsFromMemory()) {
                                    globalSettings.logLevel = previous
                                    levelIndex.intValue = when (previous) {
                                        GlobalSettings.LOG_LEVEL_NONE -> 0
                                        GlobalSettings.LOG_LEVEL_DEBUG -> 2
                                        else -> 1
                                    }
                                    WindowUtils.showToast(ConfigBinderRepository.getLastErrorOrDefault("日志级别更新失败"))
                                }
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
