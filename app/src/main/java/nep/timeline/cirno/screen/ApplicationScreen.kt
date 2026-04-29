package nep.timeline.cirno.screen

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.topjohnwu.superuser.Shell
import dev.chrisbanes.haze.hazeEffect
import nep.timeline.cirno.ApplicationActivity
import nep.timeline.cirno.configs.ConfigManager
import nep.timeline.cirno.configs.checkers.AppConfigs
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.extra.SuperSwitch
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ApplicationScreen(activity: ApplicationActivity) {
    val appName = activity.intent.getStringExtra("appName")!!
    val packageName = activity.intent.getStringExtra("packageName")!!
    val initialUserId = activity.intent.getStringExtra("userId")?.toIntOrNull() ?: 0
    val baseUserIds = activity.intent.getIntArrayExtra("userIds")
        ?.toList()
        ?.distinct()
        ?.sorted()
        ?.let { ids -> if (ids.contains(initialUserId)) ids else (ids + initialUserId).sorted() }
        ?: listOf(initialUserId)
    val availableUserIds by produceState(initialValue = baseUserIds, key1 = packageName) {
        val base = baseUserIds.toMutableSet()
        if (Shell.getShell().isRoot) {
            val hasClone = withContext(Dispatchers.IO) {
                val result = Shell.cmd("pm path --user 999 $packageName").exec()
                result.isSuccess && result.out.isNotEmpty()
            }
            if (hasClone) {
                base.add(999)
            }
        }
        value = base.toList().sorted()
    }
    var selectedUserId by remember { mutableStateOf(initialUserId) }

    val context = LocalContext.current

    val scrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())

    val isWhitelisted = remember { mutableStateOf(false) }
    val isBackgroundPlayAllowed = remember { mutableStateOf(false) }
    val isLocationUseAllowed = remember { mutableStateOf(false) }

    LaunchedEffect(packageName, selectedUserId) {
        isWhitelisted.value = AppConfigs.isWhiteApp(packageName, selectedUserId)
        isBackgroundPlayAllowed.value = AppConfigs.isBackgroundPlayAllowed(packageName, selectedUserId)
        isLocationUseAllowed.value = AppConfigs.isLocationUseAllowed(packageName, selectedUserId)
    }
    LaunchedEffect(availableUserIds) {
        if (!availableUserIds.contains(selectedUserId)) {
            selectedUserId = availableUserIds.firstOrNull() ?: initialUserId
        }
    }

    fun anyExemptionEnabled() = isBackgroundPlayAllowed.value || isLocationUseAllowed.value

    val handleToggle = { current: Boolean, newValue: Boolean, stateSetter: (Boolean) -> Unit, configSetter: () -> Unit ->
        if (current != newValue) {
            stateSetter(newValue)
            configSetter()
            activity.setResult(android.app.Activity.RESULT_OK)
            ConfigManager.manager.saveConfigSU()
        }
    }

    HazeScaffold(
        topBar = { hazeState, hazeStyle ->
            TopAppBar(
                title = appName,
                largeTitle = appName + (if (selectedUserId == 0) "" else "  #$selectedUserId"),
                color = Color.Transparent,
                modifier = Modifier
                    .hazeEffect(hazeState) { style = hazeStyle }
                    .fillMaxWidth(),
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = 16.dp
            )
        ) {
            if (availableUserIds.size > 1) {
                item {
                    SectionTitle(
                        text = "配置用户",
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    SectionCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 12.dp)
                        ) {
                            availableUserIds.forEach { userId ->
                                val selected = userId == selectedUserId
                                Surface(
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .clickable { selectedUserId = userId },
                                    color = if (selected) MiuixTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    else MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = if (userId == 0) "用户 0" else "用户 $userId",
                                            style = MiuixTheme.textStyles.body2,
                                            color = if (selected) MiuixTheme.colorScheme.primary
                                            else MiuixTheme.colorScheme.onSurfaceSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item {
                SectionTitle(
                    text = "基本设置",
                    modifier = Modifier.padding(top = 8.dp)
                )
                SectionCard {
                    SuperSwitch(
                        title = "白名单",
                        summary = "白名单应用不会被 Cirno 冻结",
                        checked = isWhitelisted.value,
                        onCheckedChange = { newValue ->
                            if (newValue && anyExemptionEnabled()) {
                                Toast.makeText(
                                    context,
                                    "白名单不能与冻结豁免同时开启",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@SuperSwitch
                            }
                            handleToggle(
                                isWhitelisted.value, newValue,
                                { isWhitelisted.value = it },
                                { AppConfigs.setWhiteApp(packageName, selectedUserId, newValue) }
                            )
                        }
                    )
                }
            }

            item {
                SectionTitle(
                    text = "冻结豁免",
                    modifier = Modifier.padding(top = 8.dp)
                )
                SectionCard {
                    SuperSwitch(
                        title = "后台播放",
                        summary = "允许应用在播放音频时不被冻结，推荐音乐类应用",
                        checked = isBackgroundPlayAllowed.value,
                        onCheckedChange = { newValue ->
                            if (newValue && isWhitelisted.value) {
                                Toast.makeText(
                                    context,
                                    "冻结豁免不能与白名单同时开启",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@SuperSwitch
                            }
                            handleToggle(
                                isBackgroundPlayAllowed.value, newValue,
                                { isBackgroundPlayAllowed.value = it },
                                { AppConfigs.setBackgroundPlayAllowed(packageName, selectedUserId, newValue) }
                            )
                        }
                    )
                    SuperSwitch(
                        title = "位置使用",
                        summary = "允许应用在使用位置信息时不被冻结，推荐导航地图类应用",
                        checked = isLocationUseAllowed.value,
                        onCheckedChange = { newValue ->
                            if (newValue && isWhitelisted.value) {
                                Toast.makeText(
                                    context,
                                    "冻结豁免不能与白名单同时开启",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@SuperSwitch
                            }
                            handleToggle(
                                isLocationUseAllowed.value, newValue,
                                { isLocationUseAllowed.value = it },
                                { AppConfigs.setLocationUseAllowed(packageName, selectedUserId, newValue) }
                            )
                        }
                    )
                }
            }
        }
    }
}
