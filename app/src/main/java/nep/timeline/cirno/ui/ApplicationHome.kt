@file:OptIn(ExperimentalScrollBarApi::class)

package nep.timeline.cirno.ui

import java.io.File
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import nep.timeline.cirno.ApplicationActivity
import nep.timeline.cirno.CommonConstants
import nep.timeline.cirno.R
import nep.timeline.cirno.configs.checkers.AppConfigs
import nep.timeline.cirno.provide.ApplicationBinder
import nep.timeline.cirno.utils.PKGUtils
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
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
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
    val hasReKernel = remember { File("/proc/rekernel").exists() }
    val isBuiltinWhitelistApp = CommonConstants.isWhitelistApps(packageName)
    val builtinWhitelistSummary = stringResource(R.string.builtin_whitelist_summary)
    val isSystemApp = remember {
        try {
            val packageInfo = activity.packageManager.getPackageInfo(packageName, 0)
            PKGUtils.isSystemApp(packageInfo.applicationInfo)
        } catch (_: Throwable) {
            false
        }
    }

    val processList = remember { mutableStateListOf<String>() }
    val processExclusions = remember { mutableStateListOf<String>() }
    val processListLoaded = remember { mutableStateOf(false) }
    val black = remember { mutableStateOf(AppConfigs.isBlackApp(packageName, userId)) }
    val white = remember { mutableStateOf(AppConfigs.isWhiteApp(packageName, userId)) }

    LaunchedEffect(packageName, userId) {
        val appBinder = ApplicationBinder.getInstance()
        if (appBinder != null) {
            try {
                val json = appBinder.getProcessesForApp(packageName, userId)
                if (json != null) {
                    val type = object : TypeToken<List<String>>() {}.type
                    val names: List<String> = Gson().fromJson(json, type) ?: emptyList()
                    processList.clear()
                    processList.addAll(names)
                }
            } catch (_: Throwable) {
            }
        }
        val excluded = AppConfigs.getExcludedProcesses(packageName, userId)
        processExclusions.clear()
        processExclusions.addAll(excluded)
        processListLoaded.value = true
    }

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
                        val backgroundPlay = remember { mutableStateOf(AppConfigs.isBackgroundPlayAllowed(packageName, userId)) }
                        val locationUse = remember { mutableStateOf(AppConfigs.isLocationUseAllowed(packageName, userId)) }
                        val networkMessage = remember { mutableStateOf(AppConfigs.isNetworkMessageAllowed(packageName, userId)) }
                        val networkSpeed = remember { mutableStateOf(AppConfigs.isNetworkSpeedAllowed(packageName, userId)) }

                        if (!hasReKernel && networkMessage.value) {
                            networkMessage.value = false
                            AppConfigs.setNetworkMessageAllowed(packageName, userId, false)
                            ConfigBinderRepository.saveApplicationSettingsFromMemory()
                        }

                        if (!isSystemApp) {
                            SwitchPreference(
                                title = stringResource(R.string.white_app),
                                summary = if (isBuiltinWhitelistApp) builtinWhitelistSummary else null,
                                checked = isBuiltinWhitelistApp || white.value,
                                enabled = !isBuiltinWhitelistApp,
                                onCheckedChange = {
                                    if (isBuiltinWhitelistApp) return@SwitchPreference
                                    val prevWhite = white.value
                                    val prevBackground = backgroundPlay.value
                                    val prevLocation = locationUse.value
                                    val prevNetwork = networkMessage.value
                                    val prevNetworkSpeed = networkSpeed.value

                                    white.value = it
                                    AppConfigs.setWhiteApp(packageName, userId, it)
                                    if (it) {
                                        backgroundPlay.value = false
                                        AppConfigs.setBackgroundPlayAllowed(packageName, userId, false)
                                        locationUse.value = false
                                        AppConfigs.setLocationUseAllowed(packageName, userId, false)
                                        networkMessage.value = false
                                        AppConfigs.setNetworkMessageAllowed(packageName, userId, false)
                                        networkSpeed.value = false
                                        AppConfigs.setNetworkSpeedAllowed(packageName, userId, false)
                                    }

                                    if (!ConfigBinderRepository.saveApplicationSettingsFromMemory()) {
                                        white.value = prevWhite
                                        AppConfigs.setWhiteApp(packageName, userId, prevWhite)
                                        backgroundPlay.value = prevBackground
                                        AppConfigs.setBackgroundPlayAllowed(packageName, userId, prevBackground)
                                        locationUse.value = prevLocation
                                        AppConfigs.setLocationUseAllowed(packageName, userId, prevLocation)
                                        networkMessage.value = prevNetwork
                                        AppConfigs.setNetworkMessageAllowed(packageName, userId, prevNetwork)
                                        networkSpeed.value = prevNetworkSpeed
                                        AppConfigs.setNetworkSpeedAllowed(packageName, userId, prevNetworkSpeed)
                                        WindowUtils.showToast(ConfigBinderRepository.getLastErrorOrDefault("白名单更新失败"))
                                    }
                                }
                            )
                        }

                        if (!isBuiltinWhitelistApp && !isSystemApp) {
                            SwitchPreference(
                                title = stringResource(R.string.background_play),
                                checked = backgroundPlay.value,
                                enabled = !white.value,
                                onCheckedChange = {
                                    if (white.value && it) {
                                        WindowUtils.showToast(stringResource(R.string.whitelist_exemption_blocked))
                                        return@SwitchPreference
                                    }
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
                                enabled = !white.value,
                                onCheckedChange = {
                                    if (white.value && it) {
                                        WindowUtils.showToast(stringResource(R.string.whitelist_exemption_blocked))
                                        return@SwitchPreference
                                    }
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
                                summary = if (hasReKernel) null else stringResource(R.string.rekernel_required_summary),
                                checked = networkMessage.value,
                                enabled = hasReKernel && !white.value,
                                onCheckedChange = {
                                    if (white.value && it) {
                                        WindowUtils.showToast(stringResource(R.string.whitelist_exemption_blocked))
                                        return@SwitchPreference
                                    }
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
                                title = stringResource(R.string.network_speed_check),
                                checked = networkSpeed.value,
                                enabled = !white.value,
                                onCheckedChange = {
                                    if (white.value && it) {
                                        WindowUtils.showToast(stringResource(R.string.whitelist_exemption_blocked))
                                        return@SwitchPreference
                                    }
                                    val previous = networkSpeed.value
                                    networkSpeed.value = it
                                    AppConfigs.setNetworkSpeedAllowed(packageName, userId, it)
                                    if (!ConfigBinderRepository.saveApplicationSettingsFromMemory()) {
                                        networkSpeed.value = previous
                                        AppConfigs.setNetworkSpeedAllowed(packageName, userId, previous)
                                        WindowUtils.showToast(ConfigBinderRepository.getLastErrorOrDefault("网速识别配置更新失败"))
                                    }
                                }
                            )
                        }

                        if (isSystemApp || isBuiltinWhitelistApp) {
                            SwitchPreference(
                                title = stringResource(R.string.black_app),
                                summary = if (isBuiltinWhitelistApp) stringResource(R.string.builtin_whitelist_blacklist_blocked) else null,
                                checked = black.value,
                                onCheckedChange = {
                                    val prevBlack = black.value

                                    black.value = it
                                    AppConfigs.setBlackApp(packageName, userId, it)

                                    if (!ConfigBinderRepository.saveApplicationSettingsFromMemory()) {
                                        black.value = prevBlack
                                        AppConfigs.setBlackApp(packageName, userId, prevBlack)
                                        WindowUtils.showToast(ConfigBinderRepository.getLastErrorOrDefault("黑名单更新失败"))
                                    }
                                }
                            )
                        }

                    }
                }

                if (processListLoaded.value && !isBuiltinWhitelistApp && !white.value && isSystemApp == black.value) {
                    item {
                        SmallTitle(text = stringResource(R.string.process_freeze_control))
                        Card(modifier = Modifier.padding(12.dp)) {
                            if (processList.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stringResource(R.string.no_process_hint),
                                        color = Color.Gray
                                    )
                                }
                            } else {
                                Text(
                                    text = stringResource(R.string.process_freeze_control_summary),
                                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                                    color = Color.Gray
                                )
                                processList.forEach { processName ->
                                    val isExcluded = remember { mutableStateOf(processExclusions.contains(processName)) }
                                    SwitchPreference(
                                        title = processName,
                                        checked = !isExcluded.value,
                                        onCheckedChange = { frozen ->
                                            val previous = isExcluded.value
                                            isExcluded.value = !frozen
                                            AppConfigs.setProcessExcludedFromFreeze(packageName, userId, processName, !frozen)
                                            if (frozen) {
                                                processExclusions.remove(processName)
                                            } else {
                                                processExclusions.add(processName)
                                            }
                                            if (!ConfigBinderRepository.saveApplicationSettingsFromMemory()) {
                                                isExcluded.value = previous
                                                AppConfigs.setProcessExcludedFromFreeze(packageName, userId, processName, previous)
                                                if (previous) {
                                                    processExclusions.add(processName)
                                                } else {
                                                    processExclusions.remove(processName)
                                                }
                                                WindowUtils.showToast(ConfigBinderRepository.getLastErrorOrDefault("进程冻结配置更新失败"))
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
