@file:OptIn(ExperimentalScrollBarApi::class)

package nep.timeline.cirno.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import nep.timeline.cirno.ApplicationActivity
import nep.timeline.cirno.R
import nep.timeline.cirno.configs.checkers.AppConfigs
import nep.timeline.cirno.ui.custom.BackNavigationIcon
import nep.timeline.cirno.ui.utils.AdaptiveTopAppBar
import nep.timeline.cirno.ui.utils.BlurredBar
import nep.timeline.cirno.ui.utils.ConfigBinderRepository
import nep.timeline.cirno.ui.utils.WindowUtils
import nep.timeline.cirno.ui.utils.pageContentPadding
import nep.timeline.cirno.ui.utils.pageScrollModifiers
import nep.timeline.cirno.ui.utils.shouldShowSplitPane
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.interfaces.ExperimentalScrollBarApi
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

@Composable
fun ApplicationHome(activity: ApplicationActivity) {
    val scrollBehavior = MiuixScrollBehavior()
    val isWideScreen = shouldShowSplitPane()
    val appName = activity.intent.getStringExtra("appName") ?: "App"
    val packageName = activity.intent.getStringExtra("packageName") ?: return
    val userId = activity.intent.getStringExtra("userId")?.toIntOrNull() ?: 0

    Scaffold(
        topBar = {
            BlurredBar(null, false, scrollBehavior) {
                AdaptiveTopAppBar(
                    title = appName,
                    isWideScreen = isWideScreen,
                    scrollBehavior = scrollBehavior,
                    color = colorScheme.surface,
                    navigationIcon = {
                        BackNavigationIcon(onClick = { activity.finish() })
                    }
                )
            }
        }
    ) { padding ->
        val lazyListState = rememberLazyListState()
        val contentPadding = pageContentPadding(padding, padding, isWideScreen)
        Box {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.pageScrollModifiers(true, true, scrollBehavior),
                contentPadding = contentPadding,
            ) {
                item {
                    Card(modifier = Modifier.padding(12.dp)) {
                        val white = remember { mutableStateOf(AppConfigs.isWhiteApp(packageName, userId)) }
                        val backgroundPlay = remember { mutableStateOf(AppConfigs.isBackgroundPlayAllowed(packageName, userId)) }
                        val locationUse = remember { mutableStateOf(AppConfigs.isLocationUseAllowed(packageName, userId)) }
                        val networkMessage = remember { mutableStateOf(AppConfigs.isNetworkMessageAllowed(packageName, userId)) }

                        SwitchPreference(
                            title = stringResource(R.string.white_app),
                            checked = white.value,
                            onCheckedChange = {
                                val previous = white.value
                                white.value = it
                                AppConfigs.setWhiteApp(packageName, userId, it)
                                if (!ConfigBinderRepository.saveApplicationSettingsFromMemory()) {
                                    white.value = previous
                                    AppConfigs.setWhiteApp(packageName, userId, previous)
                                    WindowUtils.showToast(ConfigBinderRepository.getLastErrorOrDefault("白名单更新失败"))
                                }
                            }
                        )

                        SwitchPreference(
                            title = stringResource(R.string.background_play),
                            checked = backgroundPlay.value,
                            onCheckedChange = {
                                val previous = backgroundPlay.value
                                backgroundPlay.value = it
                                AppConfigs.setBackgroundPlayAllowed(packageName, userId, it)
                                if (!ConfigBinderRepository.saveApplicationSettingsFromMemory()) {
                                    backgroundPlay.value = previous
                                    AppConfigs.setBackgroundPlayAllowed(packageName, userId, previous)
                                    WindowUtils.showToast(ConfigBinderRepository.getLastErrorOrDefault("后台播放配置更新失败"))
                                }
                            }
                        )

                        SwitchPreference(
                            title = stringResource(R.string.location_check),
                            checked = locationUse.value,
                            onCheckedChange = {
                                val previous = locationUse.value
                                locationUse.value = it
                                AppConfigs.setLocationUseAllowed(packageName, userId, it)
                                if (!ConfigBinderRepository.saveApplicationSettingsFromMemory()) {
                                    locationUse.value = previous
                                    AppConfigs.setLocationUseAllowed(packageName, userId, previous)
                                    WindowUtils.showToast(ConfigBinderRepository.getLastErrorOrDefault("定位配置更新失败"))
                                }
                            }
                        )

                        SwitchPreference(
                            title = stringResource(R.string.netreceive_unfreeze),
                            checked = networkMessage.value,
                            onCheckedChange = {
                                val previous = networkMessage.value
                                networkMessage.value = it
                                AppConfigs.setNetworkMessageAllowed(packageName, userId, it)
                                if (!ConfigBinderRepository.saveApplicationSettingsFromMemory()) {
                                    networkMessage.value = previous
                                    AppConfigs.setNetworkMessageAllowed(packageName, userId, previous)
                                    WindowUtils.showToast(ConfigBinderRepository.getLastErrorOrDefault("网络消息配置更新失败"))
                                }
                            }
                        )

                        SwitchPreference(
                            title = stringResource(R.string.black_app),
                            checked = false,
                            onCheckedChange = {
                                WindowUtils.showToast("还未实现")
                            }
                        )
                    }
                }
            }
        }
    }
}
