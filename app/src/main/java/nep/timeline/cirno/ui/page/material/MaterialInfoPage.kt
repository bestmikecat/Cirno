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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.NavigateNext
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PauseCircleOutline
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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

@OptIn(ExperimentalMaterial3Api::class)
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
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 20.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                )
            }

            item {
                val active = GlobalVars.isModuleActive
                val working = active && !binderState.hasError
                val moduleVersion = binderState.moduleVersion ?: stringResource(R.string.not_running)
                val hasWarning = !active || binderState.hasError || (
                    active && binderState.binderAvailable && binderState.moduleVersion != null && binderState.moduleVersion != BuildConfig.VERSION_NAME
                )

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (working) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.errorContainer,
                    ),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
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
                                    text = moduleVersion,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Icon(
                                imageVector = if (working) Icons.Outlined.CheckCircleOutline else Icons.Outlined.ErrorOutline,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = if (working) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            )
                        }

                        if (hasWarning) {
                            val warningText = when {
                                !active -> stringResource(R.string.not_active)
                                binderState.hasError -> stringResource(R.string.internal_error)
                                else -> stringResource(R.string.module_version_mismatch)
                            }
                            Text(
                                text = warningText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (working) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onErrorContainer,
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
            }

            item {
                val working = GlobalVars.isModuleActive && !binderState.hasError
                MaterialSectionCard {
                    MaterialInfoRow(stringResource(R.string.manager_version), "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}-${BuildConfig.BUILD_TIME})")
                    MaterialInfoRow(stringResource(R.string.hook_type), if (working) "Xposed" else stringResource(R.string.unknown))
                    MaterialInfoRow(stringResource(R.string.android_version), if (Build.VERSION.PREVIEW_SDK_INT != 0) (Build.VERSION.CODENAME + " Preview (API " + Build.VERSION.PREVIEW_SDK_INT + "/" + Build.VERSION.SDK_INT + ")") else (VersionUtils.getAndroidVersion() + " (API " + Build.VERSION.SDK_INT + ")"))
                    MaterialInfoRow(stringResource(R.string.xposed_version), if (working) GlobalVars.XposedVersion.toString() else stringResource(R.string.unknown))
                    MaterialInfoRow(stringResource(R.string.system_fingerprint), Build.FINGERPRINT, divider = false)
                }
            }

            item {
                MaterialSectionCard {
                    ListItem(
                        headlineContent = { Text(if (isCheckingUpdate) stringResource(R.string.update_checking) else stringResource(R.string.check_update)) },
                        supportingContent = { Text(stringResource(R.string.update_check_summary)) },
                        leadingContent = { Icon(Icons.Outlined.SystemUpdate, contentDescription = null) },
                        trailingContent = { Icon(Icons.AutoMirrored.Outlined.NavigateNext, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().clickable(enabled = !isCheckingUpdate) { isCheckingUpdate = true },
                    )
                }
            }

            item {
                MaterialSectionCard {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.home_about_freezer)) },
                        leadingContent = { Icon(Icons.Outlined.Info, contentDescription = null) },
                        trailingContent = { Icon(Icons.AutoMirrored.Outlined.NavigateNext, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().clickable { navigator.push(Route.About) },
                    )
                }
            }

            item {
                MaterialSectionCard {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.home_logs)) },
                        leadingContent = { Icon(Icons.Outlined.BugReport, contentDescription = null) },
                        trailingContent = { Icon(Icons.AutoMirrored.Outlined.NavigateNext, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().clickable { navigator.push(Route.Log) },
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun MaterialStatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun MaterialSectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.fillMaxWidth(), content = content)
    }
}

@Composable
private fun MaterialInfoRow(title: String, content: String, divider: Boolean = true) {
    Column(modifier = Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = { Text(title) },
            supportingContent = { Text(content, style = MaterialTheme.typography.bodyMedium) },
        )
    }
}
