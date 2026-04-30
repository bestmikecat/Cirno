package nep.timeline.cirno.screen

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.topjohnwu.superuser.Shell
import nep.timeline.cirno.ApplicationActivity
import nep.timeline.cirno.CommonConstants
import nep.timeline.cirno.GlobalVars
import nep.timeline.cirno.configs.ConfigManager
import nep.timeline.cirno.configs.checkers.AppConfigs
import nep.timeline.cirno.utils.PKGUtils
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.SearchBar
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SliderDefaults
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.extra.SuperSwitch
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.VerticalSplit
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class SpecialTag(val label: String, val color: Color)
private data class HomeAppItem(
    val appInfo: ApplicationInfo,
    val appName: String,
    val userId: Int,
    val tags: List<SpecialTag>,
    val availableUserIds: List<Int>
)

private fun buildAppIntent(context: Context, app: HomeAppItem): Intent {
    return Intent().apply {
        setClass(context, ApplicationActivity::class.java)
        putExtra("appName", app.appName)
        putExtra("userId", app.userId.toString())
        putExtra("packageName", app.appInfo.packageName)
        putExtra("userIds", app.availableUserIds.toIntArray())
    }
}

private fun getSpecialTagsForUsers(packageName: String, userIds: List<Int>): List<SpecialTag> {
    val tags = mutableListOf<SpecialTag>()
    fun addTagForUser(baseLabel: String, color: Color, userId: Int) {
        val label = if (userId == 0) baseLabel else "$baseLabel($userId)"
        tags += SpecialTag(label, color)
    }
    userIds.forEach { userId ->
        if (AppConfigs.isWhiteApp(packageName, userId)) addTagForUser("白名单", Color(0xFF4CAF50), userId)
        if (AppConfigs.isNetworkMessageAllowed(packageName, userId)) addTagForUser("网络解冻", Color(0xFF9C27B0), userId)
        if (AppConfigs.isBackgroundPlayAllowed(packageName, userId)) addTagForUser("后台播放", Color(0xFF2196F3), userId)
        if (AppConfigs.isLocationUseAllowed(packageName, userId)) addTagForUser("位置使用", Color(0xFFFF9800), userId)
    }
    return tags
}

private fun getInstalledApps(context: Context): List<ApplicationInfo> {
    val packageManager = context.packageManager
    return packageManager.getInstalledApplications(PackageManager.GET_META_DATA).filter {
        !CommonConstants.isWhitelistApps(it.packageName) && !PKGUtils.isSystemApp(it)
    }
}

@Composable
fun MainScreen() {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()
    var refreshTick by remember { mutableStateOf(0) }

    val appDetailLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            refreshTick++
        }
    }

    HazeScaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = pagerState.currentPage == 0,
                    onClick = {
                        if (pagerState.currentPage != 0) {
                            coroutineScope.launch { pagerState.animateScrollToPage(0) }
                        }
                    },
                    icon = MiuixIcons.VerticalSplit,
                    label = "主页"
                )
                NavigationBarItem(
                    selected = pagerState.currentPage == 1,
                    onClick = {
                        if (pagerState.currentPage != 1) {
                            coroutineScope.launch { pagerState.animateScrollToPage(1) }
                        }
                    },
                    icon = MiuixIcons.Settings,
                    label = "设置"
                )
            }
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> HomeTab(
                    bottomInset = innerPadding.calculateBottomPadding(),
                    refreshKey = refreshTick,
                    onOpenApp = { intent -> appDetailLauncher.launch(intent) }
                )
                1 -> SettingScreen(bottomInset = innerPadding.calculateBottomPadding())
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppItem(
    app: HomeAppItem,
    packageManager: PackageManager,
    canEnter: Boolean,
    onClick: () -> Unit
) {
    val appIcon = remember(app.appInfo.packageName, app.appInfo.uid) {
        app.appInfo.loadIcon(packageManager).toBitmap().asImageBitmap()
    }
    val tags = app.tags

    Row(
        modifier = (if (canEnter) Modifier.clickable(onClick = onClick) else Modifier)
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            bitmap = appIcon,
            contentDescription = null,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.appName,
                style = MiuixTheme.textStyles.subtitle,
                color = MiuixTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = (if (app.userId == 0) "" else "${app.userId} · ") + app.appInfo.packageName,
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (tags.isNotEmpty()) {
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

@Composable
private fun AppSectionCard(
    apps: List<HomeAppItem>,
    packageManager: PackageManager,
    canEnter: Boolean,
    onOpenApp: (HomeAppItem) -> Unit
) {
    SectionCard {
        apps.forEach { app ->
            AppItem(
                app = app,
                packageManager = packageManager,
                canEnter = canEnter,
                onClick = { onOpenApp(app) }
            )
        }
    }
}

@Composable
private fun HomeTab(
    bottomInset: Dp = 0.dp,
    refreshKey: Int = 0,
    onOpenApp: (Intent) -> Unit
) {
    val context = LocalContext.current

    val isRootState by produceState<Boolean?>(initialValue = null) {
        value = withContext(Dispatchers.IO) {
            Shell.getShell().isRoot
        }
    }

    val readConfigState by produceState<Boolean?>(initialValue = null, key1 = refreshKey) {
        value = withContext(Dispatchers.IO) {
            ConfigManager.manager.readConfigSU()
        }
    }

    var searchText by remember { mutableStateOf("") }

    val apps by produceState<List<HomeAppItem>?>(initialValue = null, key1 = refreshKey, key2 = readConfigState) {
        if (readConfigState == null) {
            value = null
            return@produceState
        }
        value = withContext(Dispatchers.Default) {
            val appInfos = getInstalledApps(context)
            val userIdsByPackage = appInfos
                .groupBy { it.packageName }
                .mapValues { entry ->
                    entry.value
                        .map { PKGUtils.getUserId(it.uid) }
                        .distinct()
                        .sorted()
                }

            appInfos
                .map { appInfo ->
                    val userId = PKGUtils.getUserId(appInfo.uid)
                    val tagUserIds = listOf(0, 999, userId).distinct()
                    val availableUserIds = userIdsByPackage[appInfo.packageName] ?: listOf(userId)
                    HomeAppItem(
                        appInfo = appInfo,
                        appName = appInfo.loadLabel(context.packageManager).toString(),
                        userId = userId,
                        tags = getSpecialTagsForUsers(appInfo.packageName, tagUserIds),
                        availableUserIds = availableUserIds
                    )
                }
                .sortedBy { it.appName.lowercase() }
        }
    }

    LaunchedEffect(isRootState) {
        if (isRootState == false) {
            Toast.makeText(
                context,
                "检测到您未授予 Cirno Root 权限，UI 管理功能无法使用",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val filteredApps by remember(apps, searchText) {
        derivedStateOf {
            if (searchText.isBlank()) apps
            else apps?.filter {
                it.appName.contains(searchText, ignoreCase = true) ||
                    it.appInfo.packageName.contains(searchText, ignoreCase = true)
            }
        }
    }

    val partitionedApps by remember(filteredApps) {
        derivedStateOf {
            filteredApps?.partition { it.tags.isNotEmpty() }
        }
    }

    HazeScaffold(
        topBar = { hazeState, hazeStyle ->
            HazeTopBar(hazeState = hazeState, hazeStyle = hazeStyle) {
                SmallTopAppBar(
                    title = "Cirno",
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    ) { padding ->
        val allFiltered = filteredApps
        if (allFiltered == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                InfiniteProgressIndicator()
            }
            return@HazeScaffold
        }

        val packageManager = context.packageManager
        val canEnter = isRootState == true && readConfigState == true
        val (specialApps, normalApps) = partitionedApps ?: (emptyList<HomeAppItem>() to emptyList())

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + bottomInset + 16.dp
            )
        ) {
            item {
                SearchBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(top = 4.dp),
                    inputField = {
                        InputField(
                            query = searchText,
                            onQueryChange = { searchText = it },
                            onSearch = { },
                            expanded = searchText.isNotEmpty(),
                            onExpandedChange = { },
                            label = "搜索应用"
                        )
                    },
                    expanded = searchText.isNotEmpty(),
                    onExpandedChange = { },
                    outsideEndAction = if (searchText.isNotEmpty()) {
                        {
                            Text(
                                modifier = Modifier
                                    .padding(start = 12.dp)
                                    .clickable(
                                        interactionSource = null,
                                        indication = null
                                    ) {
                                        searchText = ""
                                    },
                                text = "清除",
                                color = MiuixTheme.colorScheme.primary
                            )
                        }
                    } else null,
                    content = {}
                )
            }

            item {
                SectionCard(topPadding = 12.dp) {
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
                                    color = if (GlobalVars.isModuleActive) Color(0xFF4CAF50).copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f),
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

            if (normalApps.isEmpty() && specialApps.isEmpty() && searchText.isNotEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "未找到相关应用",
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            if (specialApps.isNotEmpty()) {
                item {
                    SectionTitle(text = "特殊配置应用", modifier = Modifier.padding(top = 8.dp))
                }
                item {
                    AppSectionCard(
                        apps = specialApps,
                        packageManager = packageManager,
                        canEnter = canEnter,
                        onOpenApp = { app -> onOpenApp(buildAppIntent(context, app)) }
                    )
                }
            }

            item {
                SectionTitle(
                    text = if (searchText.isNotEmpty()) "搜索结果" else "全部应用",
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            item {
                AppSectionCard(
                    apps = normalApps,
                    packageManager = packageManager,
                    canEnter = canEnter,
                    onOpenApp = { app -> onOpenApp(buildAppIntent(context, app)) }
                )
            }
        }
    }
}

@Composable
fun SettingScreen(bottomInset: Dp = 0.dp) {
    val scrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())

    val freezeDelay = remember { mutableStateOf(GlobalVars.globalSettings.freezeDelay) }
    val logEnabled = remember { mutableStateOf(GlobalVars.globalSettings.logEnabled) }

    HazeScaffold(
        topBar = { hazeState, hazeStyle ->
            HazeTopBar(hazeState = hazeState, hazeStyle = hazeStyle) {
                SmallTopAppBar(
                    title = "设置",
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxWidth(),
                    scrollBehavior = scrollBehavior
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + bottomInset + 16.dp
            )
        ) {
            item {
                SectionTitle(
                    text = "冻结",
                    modifier = Modifier.padding(top = 8.dp)
                )
                SectionCard {
                    BasicComponent(
                        title = "冻结延时",
                        summary = "应用进入后台后延迟冻结的时间，单位秒",
                        endActions = {
                            Text(
                                text = "${freezeDelay.value}",
                                fontSize = MiuixTheme.textStyles.body2.fontSize,
                                color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                            )
                        },
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
                SectionTitle(
                    text = "调试",
                    modifier = Modifier.padding(top = 8.dp)
                )
                SectionCard {
                    SuperSwitch(
                        title = "日志输出",
                        summary = "在 /data/system/Cirno/log 中输出 Cirno 日志",
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
