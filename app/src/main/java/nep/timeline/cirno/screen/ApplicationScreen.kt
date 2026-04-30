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
import nep.timeline.cirno.ApplicationActivity
import nep.timeline.cirno.configs.ConfigManager
import nep.timeline.cirno.configs.checkers.AppConfigs
import nep.timeline.cirno.configs.policy.Capability
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.extra.SuperSwitch
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class CapabilitySpec(
    val capability: Capability,
    val title: String,
    val summary: String
)

private val baseCapabilitySpecs = listOf(
    CapabilitySpec(
        capability = Capability.WHITE_LIST,
        title = "白名单",
        summary = "白名单应用不会被 Cirno 冻结"
    )
)

private val exemptionCapabilitySpecs = listOf(
    CapabilitySpec(
        capability = Capability.ALLOW_NETWORK_MESSAGE,
        title = "网络解冻",
        summary = "应用收到网络通知时临时解冻，冻结时保留 TCP 连接，需使用 ReKernel，推荐微信"
    ),
    CapabilitySpec(
        capability = Capability.ALLOW_BACKGROUND_AUDIO,
        title = "后台播放",
        summary = "允许应用在播放音频时不被冻结，推荐音乐类应用"
    ),
    CapabilitySpec(
        capability = Capability.ALLOW_LOCATION,
        title = "位置使用",
        summary = "允许应用在使用位置信息时不被冻结，推荐导航地图类应用"
    )
)

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

    val allSpecs = remember { baseCapabilitySpecs + exemptionCapabilitySpecs }
    val capabilityStates = remember {
        mutableStateOf(allSpecs.associate { it.capability to false })
    }

    LaunchedEffect(packageName, selectedUserId) {
        val nextState = allSpecs.associate { spec ->
            spec.capability to AppConfigs.hasCapability(packageName, selectedUserId, spec.capability)
        }
        capabilityStates.value = nextState
    }
    LaunchedEffect(availableUserIds) {
        if (!availableUserIds.contains(selectedUserId)) {
            selectedUserId = availableUserIds.firstOrNull() ?: initialUserId
        }
    }

    fun isEnabled(capability: Capability): Boolean = capabilityStates.value[capability] == true

    fun anyExemptionEnabled(): Boolean = exemptionCapabilitySpecs.any { isEnabled(it.capability) }

    fun applyCapability(spec: CapabilitySpec, newValue: Boolean) {
        val current = isEnabled(spec.capability)
        if (current == newValue) return

        if (newValue) {
            val isWhiteList = spec.capability == Capability.WHITE_LIST
            val whiteListEnabled = isEnabled(Capability.WHITE_LIST)
            val conflict = (isWhiteList && anyExemptionEnabled()) || (!isWhiteList && whiteListEnabled)

            if (conflict) {
                Toast.makeText(
                    context,
                    "白名单不能与任何冻结豁免同时开启",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
        }

        AppConfigs.setCapability(packageName, selectedUserId, spec.capability, newValue)
        capabilityStates.value = capabilityStates.value.toMutableMap().apply {
            put(spec.capability, newValue)
        }
        activity.setResult(android.app.Activity.RESULT_OK)
        ConfigManager.manager.saveConfigSU()
    }

    HazeScaffold(
        topBar = { hazeState, hazeStyle ->
            HazeTopBar(hazeState = hazeState, hazeStyle = hazeStyle) {
                TopAppBar(
                    title = appName,
                    largeTitle = appName + (if (selectedUserId == 0) "" else "  #$selectedUserId"),
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
                    baseCapabilitySpecs.forEach { spec ->
                        SuperSwitch(
                            title = spec.title,
                            summary = spec.summary,
                            checked = isEnabled(spec.capability),
                            onCheckedChange = { newValue ->
                                applyCapability(spec, newValue)
                            }
                        )
                    }
                }
            }

            item {
                SectionTitle(
                    text = "冻结豁免",
                    modifier = Modifier.padding(top = 8.dp)
                )
                SectionCard {
                    exemptionCapabilitySpecs.forEach { spec ->
                        SuperSwitch(
                            title = spec.title,
                            summary = spec.summary,
                            checked = isEnabled(spec.capability),
                            onCheckedChange = { newValue ->
                                applyCapability(spec, newValue)
                            }
                        )
                    }
                }
            }
        }
    }
}
