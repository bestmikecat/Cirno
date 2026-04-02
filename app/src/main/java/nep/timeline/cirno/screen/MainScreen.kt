package nep.timeline.cirno.screen

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.topjohnwu.superuser.Shell
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.VerticalSplit
import top.yukonga.miuix.kmp.theme.MiuixTheme
import nep.timeline.cirno.ApplicationActivity
import nep.timeline.cirno.CommonConstants
import nep.timeline.cirno.GlobalVars
import nep.timeline.cirno.configs.ConfigManager
import nep.timeline.cirno.configs.checkers.AppConfigs
import nep.timeline.cirno.utils.PKGUtils
import kotlin.io.encoding.ExperimentalEncodingApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.extra.SuperArrow
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.extra.SuperSwitch
import top.yukonga.miuix.kmp.basic.SliderDefaults
import top.yukonga.miuix.kmp.basic.BasicComponent

// 特殊配置标签定义
private data class SpecialTag(val label: String, val color: Color)

private fun getSpecialTags(packageName: String, userId: Int): List<SpecialTag> {
    val tags = mutableListOf<SpecialTag>()
    if (AppConfigs.isWhiteApp(packageName, userId))
        tags += SpecialTag("白名单", Color(0xFF4CAF50))
    if (AppConfigs.isBackgroundPlayAllowed(packageName, userId))
        tags += SpecialTag("后台播放", Color(0xFF2196F3))
    if (AppConfigs.isLocationUseAllowed(packageName, userId))
        tags += SpecialTag("位置使用", Color(0xFFFF9800))
    return tags
}

@OptIn(ExperimentalEncodingApi::class, ExperimentalLayoutApi::class)
@Composable
fun MainScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }

    val bottomBar: @Composable () -> Unit = {
        NavigationBar {
            NavigationBarItem(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                icon = MiuixIcons.VerticalSplit,
                label = "主页"
            )
            NavigationBarItem(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                icon = MiuixIcons.Settings,
                label = "设置"
            )
        }
    }

    when (selectedTab) {
        0 -> HomeTab(bottomBar = bottomBar)
        1 -> SettingScreen(bottomBar = bottomBar)
    }
}

@OptIn(ExperimentalEncodingApi::class, ExperimentalLayoutApi::class)
@Composable
private fun HomeTab(bottomBar: @Composable () -> Unit) {
    val handler = Handler(Looper.getMainLooper())

    val hazeState = rememberHazeState()
    val hazeStyle = HazeStyle(
        backgroundColor = MiuixTheme.colorScheme.background,
        tint = HazeTint(MiuixTheme.colorScheme.background.copy(0.67f))
    )

    val context = LocalContext.current

    fun getInstalledApps(context: Context): List<ApplicationInfo> {
        val packageManager = context.packageManager
        return packageManager.getInstalledApplications(PackageManager.GET_META_DATA).filter {
            !CommonConstants.isWhitelistApps(it.packageName) && !PKGUtils.isSystemApp(it)
        }
    }

    val readConfig = ConfigManager.manager.readConfigSU()
    val apps = remember { mutableStateOf<List<ApplicationInfo>>(emptyList()) }

    LaunchedEffect(Unit) {
        handler.post { apps.value = getInstalledApps(context) }
        if (!Shell.getShell().isRoot) {
            Toast.makeText(context, "检测到您未授予 Cirno Root 权限，UI 管理功能无法使用", Toast.LENGTH_SHORT).show()
        }
    }

    fun enterAppPage(appName: String, userId: String, packageName: String) {
        val intent = Intent()
        intent.setClass(context, ApplicationActivity::class.java)
        intent.putExtra("appName", appName)
        intent.putExtra("userId", userId)
        intent.putExtra("packageName", packageName)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    @Composable
    fun AppItem(appInfo: ApplicationInfo, packageManager: PackageManager) {
        val appName = appInfo.loadLabel(packageManager).toString()
        val appIcon = appInfo.loadIcon(packageManager)
        val userId = PKGUtils.getUserId(appInfo.uid)
        val tags = getSpecialTags(appInfo.packageName, userId)
        val hasAnyTag = tags.isNotEmpty()
        val primaryTagColor = tags.firstOrNull()?.color

        Row(
            modifier = (if (Shell.getShell().isRoot && readConfig)
                Modifier.clickable { enterAppPage(appName, userId.toString(), appInfo.packageName) }
            else Modifier)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                bitmap = appIcon.toBitmap().asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appName,
                    style = MiuixTheme.textStyles.subtitle,
                    color = primaryTagColor ?: MiuixTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = (if (userId == 0) "" else "$userId · ") + appInfo.packageName,
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (hasAnyTag) {
                Spacer(modifier = Modifier.width(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    tags.forEach { tag ->
                        Box(
                            modifier = Modifier
                                .background(tag.color.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = tag.label,
                                style = MiuixTheme.textStyles.footnote2,
                                color = tag.color
                            )
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "Cirno",
                color = Color.Transparent,
                modifier = Modifier
                    .hazeEffect(hazeState) { style = hazeStyle }
                    .fillMaxWidth()
            )
        },
        bottomBar = bottomBar
    ) { padding ->
        Surface(
            modifier = Modifier
                .hazeSource(state = hazeState)
                .fillMaxSize(),
            color = MiuixTheme.colorScheme.background
        ) {
            if (apps.value.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MiuixTheme.colorScheme.primary)
                }
                return@Surface
            }

            val packageManager = context.packageManager

            val specialApps = apps.value.filter { appInfo ->
                val uid = PKGUtils.getUserId(appInfo.uid)
                getSpecialTags(appInfo.packageName, uid).isNotEmpty()
            }
            val normalApps = apps.value.filter { appInfo ->
                val uid = PKGUtils.getUserId(appInfo.uid)
                getSpecialTags(appInfo.packageName, uid).isEmpty()
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + 16.dp
                )
            ) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(top = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "模块状态",
                                    style = MiuixTheme.textStyles.subtitle
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (GlobalVars.isModuleActive) "Cirno 已激活，功能正常运行" else "Cirno 未激活，请检查模块状态",
                                    style = MiuixTheme.textStyles.footnote1,
                                    color = MiuixTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (GlobalVars.isModuleActive)
                                            Color(0xFF4CAF50).copy(alpha = 0.15f)
                                        else Color.Gray.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .background(
                                                color = if (GlobalVars.isModuleActive) Color(0xFF4CAF50) else Color.Gray,
                                                shape = CircleShape
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = if (GlobalVars.isModuleActive) "已激活" else "未激活",
                                        style = MiuixTheme.textStyles.footnote1,
                                        color = if (GlobalVars.isModuleActive) Color(0xFF4CAF50) else Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }

                if (specialApps.isNotEmpty()) {
                    item {
                        SmallTitle(text = "特殊配置应用", modifier = Modifier.padding(top = 8.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                        ) {
                            specialApps.forEach { appInfo ->
                                AppItem(appInfo, packageManager)
                            }
                        }
                    }
                }

                item {
                    SmallTitle(text = "全部应用", modifier = Modifier.padding(top = 8.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                    ) {
                        normalApps.forEach { appInfo ->
                            AppItem(appInfo, packageManager)
                        }
                    }
                }
            }
        }
    }
}

@Composable
public fun SettingScreen(bottomBar: @Composable () -> Unit) {
    val hazeState = rememberHazeState()
    val hazeStyle = HazeStyle(
        backgroundColor = MiuixTheme.colorScheme.background,
        tint = HazeTint(MiuixTheme.colorScheme.background.copy(0.67f))
    )

    val scrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())

    // 冻结延时状态（秒）
    val freezeDelay = remember {
        mutableStateOf(GlobalVars.globalSettings.freezeDelay)
    }
    var showFreezeDelayDialog by remember { mutableStateOf(false) }

    // 日志输出状态
    val logEnabled = remember {
        mutableStateOf(GlobalVars.globalSettings.logEnabled)
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "设置",
                color = Color.Transparent,
                modifier = Modifier
                    .hazeEffect(hazeState) { style = hazeStyle }
                    .fillMaxWidth(),
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = bottomBar
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
                    bottom = padding.calculateBottomPadding() + 16.dp
                )
            ) {
                item {
                    SmallTitle(
                        text = "冻结",
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                    ) {
                        BasicComponent(
                title = "冻结延时",
                summary = "应用进入后台后延迟冻结的时间，单位秒",
                endActions = { Text(
        text = "${freezeDelay.value}",
        fontSize = MiuixTheme.textStyles.body2.fontSize,
        color = MiuixTheme.colorScheme.onSurfaceVariantActions,
    ) },
                bottomAction = {
                    Slider(
                value = freezeDelay.value.toFloat(),
                onValueChange = { 
                freezeDelay.value = it.toInt()
                GlobalVars.globalSettings.freezeDelay = freezeDelay.value
                ConfigManager.manager.saveConfigSU()
                 },
                valueRange = 0f..10f,
                steps = 9,
                hapticEffect = SliderDefaults.SliderHapticEffect.Step,
                showKeyPoints = true,
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 12.dp),
            )
                },
                insideMargin = PaddingValues(16.dp, 16.dp, 16.dp, 0.dp),
            )
                    }
                }

                item {
                    SmallTitle(
                        text = "调试",
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                    ) {
                        SuperSwitch(
                            title = "日志输出",
                            summary = "在 Logcat 中输出 Cirno 运行日志",
                            checked = logEnabled.value,
                            onCheckedChange = { newValue ->
                                logEnabled.value = newValue
                                GlobalVars.globalSettings.logEnabled = newValue
                                ConfigManager.manager.saveConfigSU()
                            }
                        )
                    }
                }
            }
        }
    }
}