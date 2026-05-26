package nep.timeline.cirno.ui.page.material

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.PauseCircleOutline
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nep.timeline.cirno.BuildConfig
import nep.timeline.cirno.GlobalVars
import nep.timeline.cirno.MainActivity.AppListViewModelSingleton.appListViewModel
import nep.timeline.cirno.R
import nep.timeline.cirno.ui.app.LocalNavigator
import nep.timeline.cirno.ui.navigation3.Route
import nep.timeline.cirno.ui.utils.ConfigBinderRepository
import nep.timeline.cirno.ui.utils.UpdateChecker
import nep.timeline.cirno.ui.utils.WindowUtils
import nep.timeline.cirno.utils.VersionUtils

private data class MaterialInfoBinderState(
    val binderAvailable: Boolean = false,
    val hasError: Boolean = false,
    val moduleVersion: String? = null,
)

@Composable
fun MaterialInfoPage(
    callback: (Int) -> Unit,
    padding: PaddingValues,
) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var updateAvailable by remember { mutableStateOf(false) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var binderState by remember { mutableStateOf(MaterialInfoBinderState()) }

    LaunchedEffect(Unit) {
        binderState = withContext(Dispatchers.IO) {
            var snapshot = ConfigBinderRepository.loadInfoBinderSnapshot()
            for (attempt in 0 until 5) {
                if (snapshot.binderAvailable) break
                delay(300)
                snapshot = ConfigBinderRepository.loadInfoBinderSnapshot()
            }
            MaterialInfoBinderState(
                binderAvailable = snapshot.binderAvailable,
                hasError = snapshot.hasError,
                moduleVersion = snapshot.moduleVersion,
            )
        }
        val result = UpdateChecker.checkForUpdate()
        if (result != null && !UpdateChecker.isSkipped(context, result.versionName)) {
            updateAvailable = true
        }
    }

    LaunchedEffect(updateAvailable) {
        if (updateAvailable) {
            WindowUtils.showToast(context.getString(R.string.update_available))
        }
    }

    LaunchedEffect(isCheckingUpdate) {
        if (!isCheckingUpdate) return@LaunchedEffect
        scope.launch {
            val result = UpdateChecker.checkForUpdate()
            isCheckingUpdate = false
            if (result == null || UpdateChecker.isSkipped(context, result.versionName)) {
                WindowUtils.showToast(context.getString(R.string.update_already_latest))
            } else {
                updateAvailable = true
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                val active = GlobalVars.isModuleActive
                val working = active && !binderState.hasError
                val moduleVersion = binderState.moduleVersion ?: stringResource(R.string.not_running)
                val versionMismatch = active && binderState.binderAvailable && binderState.moduleVersion != null && binderState.moduleVersion != BuildConfig.VERSION_NAME

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (!active) {
                        MaterialWarningCard(stringResource(R.string.not_active))
                    }
                    if (binderState.hasError) {
                        MaterialWarningCard(stringResource(R.string.internal_error))
                    }
                    if (versionMismatch) {
                        MaterialWarningCard(stringResource(R.string.module_version_mismatch))
                    }

                    Card(colors = CardDefaults.cardColors(containerColor = if (working) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer)) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = if (working) stringResource(R.string.working) else stringResource(R.string.error),
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        text = stringResource(R.string.version) + ": " + moduleVersion,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Icon(
                                    imageVector = if (working) Icons.Outlined.TaskAlt else if (active) Icons.Outlined.ErrorOutline else Icons.Outlined.PauseCircleOutline,
                                    contentDescription = null,
                                    tint = if (working) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                MaterialStatCard(
                                    title = stringResource(R.string.white_app),
                                    value = if (!active || GlobalVars.applicationSettings == null) "N/A" else GlobalVars.applicationSettings.whiteApps.size.toString(),
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        appListViewModel.updateByQuery(type = 0)
                                        callback(1)
                                    },
                                )
                                MaterialStatCard(
                                    title = stringResource(R.string.black_app),
                                    value = if (!active || GlobalVars.applicationSettings == null) "N/A" else GlobalVars.applicationSettings.blackApps.size.toString(),
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        appListViewModel.updateByQuery(type = 1)
                                        callback(1)
                                    },
                                )
                            }
                        }
                    }

                    MaterialSectionCard {
                        MaterialInfoRow(stringResource(R.string.manager_version), "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}-${BuildConfig.BUILD_TIME})")
                        MaterialInfoRow(stringResource(R.string.hook_type), if (working) "Xposed" else stringResource(R.string.unknown))
                        MaterialInfoRow(stringResource(R.string.android_version), if (Build.VERSION.PREVIEW_SDK_INT != 0) (Build.VERSION.CODENAME + " Preview (API " + Build.VERSION.PREVIEW_SDK_INT + "/" + Build.VERSION.SDK_INT + ")") else (VersionUtils.getAndroidVersion() + " (API " + Build.VERSION.SDK_INT + ")"))
                        MaterialInfoRow(stringResource(R.string.xposed_version), if (working) GlobalVars.XposedVersion.toString() else stringResource(R.string.unknown))
                        MaterialInfoRow(stringResource(R.string.system_fingerprint), Build.FINGERPRINT, divider = false)
                    }

                    MaterialSectionCard {
                        ListItem(
                            headlineContent = { Text(if (isCheckingUpdate) stringResource(R.string.update_checking) else stringResource(R.string.check_update)) },
                            supportingContent = { Text(stringResource(R.string.update_check_summary)) },
                            modifier = Modifier.fillMaxWidth().clickable(enabled = !isCheckingUpdate) { isCheckingUpdate = true },
                        )
                    }

                    MaterialSectionCard {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.home_about_freezer)) },
                            supportingContent = { Text(stringResource(R.string.home_click_to_learn_freezer)) },
                            modifier = Modifier.fillMaxWidth().clickable { navigator.push(Route.About) },
                        )
                    }

                    MaterialSectionCard {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.home_logs)) },
                            supportingContent = { Text(stringResource(R.string.home_logs_desc)) },
                            modifier = Modifier.fillMaxWidth().clickable { navigator.push(Route.Log) },
                        )
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun MaterialWarningCard(message: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun MaterialStatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(modifier = modifier.clickable(onClick = onClick)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun MaterialSectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(modifier = Modifier.fillMaxWidth(), content = content)
    }
}

@Composable
private fun MaterialInfoRow(title: String, content: String, divider: Boolean = true) {
    Column(modifier = Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = { Text(title) },
            supportingContent = { Text(content, lineHeight = 20.sp) },
        )
        if (divider) {
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}
