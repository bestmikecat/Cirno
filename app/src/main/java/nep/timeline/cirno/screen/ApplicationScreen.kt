package nep.timeline.cirno.screen

import android.widget.Toast
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import nep.timeline.cirno.ApplicationActivity
import nep.timeline.cirno.configs.ConfigManager
import nep.timeline.cirno.configs.checkers.AppConfigs
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.extra.SuperSwitch
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
@Composable
fun ApplicationScreen(activity: ApplicationActivity) {
    val appName = activity.intent.getStringExtra("appName")!!
    val packageName = activity.intent.getStringExtra("packageName")!!
    var userId = activity.intent.getStringExtra("userId")
    if (userId == null) userId = "0"

    val context = LocalContext.current

    val hazeState = rememberHazeState()
    val hazeStyle = HazeStyle(
        backgroundColor = MiuixTheme.colorScheme.background,
        tint = HazeTint(MiuixTheme.colorScheme.background.copy(0.67f))
    )

    val scrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())

    val isWhitelisted =
        remember { mutableStateOf(AppConfigs.isWhiteApp(packageName, userId.toInt())) }
    val isBackgroundPlayAllowed =
        remember { mutableStateOf(AppConfigs.isBackgroundPlayAllowed(packageName, userId.toInt())) }
    val isLocationUseAllowed =
        remember { mutableStateOf(AppConfigs.isLocationUseAllowed(packageName, userId.toInt())) }

    // 冻结豁免任意一项是否已开启
    fun anyExemptionEnabled() = isBackgroundPlayAllowed.value || isLocationUseAllowed.value

    Scaffold(
        topBar = {
            TopAppBar(
                title = appName,
                largeTitle = appName + (if (userId == "0") "" else "  #$userId"),
                color = Color.Transparent,
                modifier = Modifier
                    .hazeEffect(hazeState) { style = hazeStyle }
                    .fillMaxWidth(),
                scrollBehavior = scrollBehavior
            )
        },
    ) { padding ->
        Surface(
            modifier = Modifier
                .hazeSource(state = hazeState)
                .fillMaxSize(),
            color = MiuixTheme.colorScheme.background
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding(),
                    bottom = 16.dp
                )
            ) {
                item {
                    SmallTitle(
                        text = "基本设置",
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                    ) {
                        SuperSwitch(
                            title = "白名单",
                            summary = "白名单应用不会被 Cirno 冻结",
                            checked = isWhitelisted.value,
                            onCheckedChange = { newValue ->
                                // 尝试开启白名单时，检查冻结豁免是否有项已开启
                                if (newValue && anyExemptionEnabled()) {
                                    Toast.makeText(
                                        context,
                                        "白名单不能与冻结豁免同时开启",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@SuperSwitch
                                }
                                isWhitelisted.value = newValue
                                AppConfigs.setWhiteApp(packageName, userId.toInt(), newValue)
                                ConfigManager.manager.saveConfigSU()
                            }
                        )
                    }
                }

                item {
                    SmallTitle(
                        text = "冻结豁免",
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                    ) {
                        SuperSwitch(
                            title = "后台播放",
                            summary = "允许应用在播放音频时不被冻结，推荐音乐类应用",
                            checked = isBackgroundPlayAllowed.value,
                            onCheckedChange = { newValue ->
                                // 尝试开启冻结豁免时，检查白名单是否已开启
                                if (newValue && isWhitelisted.value) {
                                    Toast.makeText(
                                        context,
                                        "冻结豁免不能与白名单同时开启",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@SuperSwitch
                                }
                                isBackgroundPlayAllowed.value = newValue
                                AppConfigs.setBackgroundPlayAllowed(
                                    packageName,
                                    userId.toInt(),
                                    newValue
                                )
                                ConfigManager.manager.saveConfigSU()
                            }
                        )
                        SuperSwitch(
                            title = "位置使用",
                            summary = "允许应用在使用位置信息时不被冻结，推荐导航地图类应用",
                            checked = isLocationUseAllowed.value,
                            onCheckedChange = { newValue ->
                                // 尝试开启冻结豁免时，检查白名单是否已开启
                                if (newValue && isWhitelisted.value) {
                                    Toast.makeText(
                                        context,
                                        "冻结豁免不能与白名单同时开启",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@SuperSwitch
                                }
                                isLocationUseAllowed.value = newValue
                                AppConfigs.setLocationUseAllowed(
                                    packageName,
                                    userId.toInt(),
                                    newValue
                                )
                                ConfigManager.manager.saveConfigSU()
                            }
                        )
                    }
                }
            }
        }
    }
}
