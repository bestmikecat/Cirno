package nep.timeline.cirno.ui.page.material

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import nep.timeline.cirno.R
import nep.timeline.cirno.ui.app.LocalNavigator
import nep.timeline.cirno.ui.dialog.DownloadProgressDialog
import nep.timeline.cirno.ui.navigation3.Route
import nep.timeline.cirno.ui.utils.ApkInstaller
import nep.timeline.cirno.ui.utils.HookStatusRepository
import nep.timeline.cirno.ui.utils.RootFreezerRepository
import nep.timeline.cirno.ui.utils.UpdateChecker
import nep.timeline.cirno.ui.utils.UpdateResult
import nep.timeline.cirno.ui.utils.WindowUtils
import nep.timeline.cirno.utils.VersionUtils

private data class MaterialHookStatusState(
    val statusBinderAvailable: Boolean = false,
    val hasError: Boolean = false,
    val freezerAvailable: Boolean = true,
    val hookVersion: String? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialInfoPage(
    padding: PaddingValues,
) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var updateResult by remember { mutableStateOf<UpdateResult?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var binderState by remember { mutableStateOf(MaterialHookStatusState()) }

    LaunchedEffect(Unit) {
        binderState = withContext(Dispatchers.IO) {
            var snapshot = HookStatusRepository.loadHookStatusSnapshot()
            for (attempt in 0 until 5) {
                if (snapshot.statusBinderAvailable) break
                delay(300)
                snapshot = HookStatusRepository.loadHookStatusSnapshot()
            }
            MaterialHookStatusState(
                statusBinderAvailable = snapshot.statusBinderAvailable,
                hasError = snapshot.hasError,
                freezerAvailable = !snapshot.statusBinderAvailable || RootFreezerRepository.isAnyFreezerAvailable(),
                hookVersion = snapshot.hookVersion,
            )
        }
        val result = UpdateChecker.checkForUpdate()
        if (result != null && !UpdateChecker.isSkipped(context, result.versionName)) {
            updateResult = result
            showUpdateDialog = true
        }
    }

    val updateAlreadyLatestText = stringResource(R.string.update_already_latest)

    LaunchedEffect(isCheckingUpdate) {
        if (!isCheckingUpdate) return@LaunchedEffect
        scope.launch {
            val result = UpdateChecker.checkForUpdate()
            isCheckingUpdate = false
            if (result == null) {
                WindowUtils.showToast(updateAlreadyLatestText)
            } else {
                updateResult = result
                showUpdateDialog = true
            }
        }
    }

    updateResult?.let { result ->
        if (showUpdateDialog) {
            MaterialUpdateDialog(
                show = true,
                updateResult = result,
                onDismissRequest = { showUpdateDialog = false },
            )
        }
    }

    MaterialPageScaffold(
        title = stringResource(R.string.app_name),
        padding = padding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            val active = GlobalVars.isModuleActive
            val working = active && !binderState.hasError
            val hookVersion = binderState.hookVersion ?: stringResource(R.string.not_running)

            MaterialSurfaceCard(
                containerColor = if (working) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer,
                contentPadding = PaddingValues(20.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.size(40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (working) Icons.Outlined.CheckCircleOutline else Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = if (working) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        )
                    }
                    Spacer(modifier = Modifier.size(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (working) stringResource(R.string.working) else stringResource(R.string.error),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = hookVersion,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item {
            val active = GlobalVars.isModuleActive
            val versionMismatch = active && binderState.statusBinderAvailable &&
                binderState.hookVersion != null && binderState.hookVersion != BuildConfig.VERSION_NAME
            if (versionMismatch) {
                MaterialWarningCard(stringResource(R.string.module_version_mismatch))
            } else {
                if (!active) {
                    MaterialWarningCard(stringResource(R.string.not_active))
                }
                if (binderState.hasError) {
                    MaterialWarningCard(stringResource(R.string.internal_error))
                }
                if (active && binderState.statusBinderAvailable && !binderState.freezerAvailable) {
                    MaterialWarningCard(stringResource(R.string.freezer_v2_unavailable))
                }
            }
        }

        item {
            val working = GlobalVars.isModuleActive && !binderState.hasError
            MaterialSurfaceCard(
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                MaterialInfoRow(stringResource(R.string.manager_version), "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}-${BuildConfig.BUILD_TIME})")
                MaterialInfoRow(stringResource(R.string.hook_type), if (working) "Xposed" else stringResource(R.string.unknown))
                MaterialInfoRow(stringResource(R.string.android_version), if (Build.VERSION.PREVIEW_SDK_INT != 0) (Build.VERSION.CODENAME + " Preview (API " + Build.VERSION.PREVIEW_SDK_INT + "/" + Build.VERSION.SDK_INT + ")") else (VersionUtils.getAndroidVersion() + " (API " + Build.VERSION.SDK_INT + ")"))
                MaterialInfoRow(stringResource(R.string.xposed_version), if (working) GlobalVars.XposedVersion.toString() else stringResource(R.string.unknown))
                MaterialInfoRow(stringResource(R.string.system_fingerprint), Build.FINGERPRINT)
            }
        }

        item {
            MaterialNavigationCard(
                title = if (isCheckingUpdate) stringResource(R.string.update_checking) else stringResource(R.string.check_update),
                icon = { MaterialIcon(Icons.Outlined.SystemUpdate) },
                enabled = !isCheckingUpdate,
                onClick = { isCheckingUpdate = true },
            )
        }

        item {
            MaterialNavigationCard(
                title = stringResource(R.string.home_logs),
                icon = { MaterialIcon(Icons.Outlined.BugReport) },
                onClick = { navigator.push(Route.Log) },
            )
        }

        item {
            MaterialNavigationCard(
                title = stringResource(R.string.home_about_freezer),
                icon = { MaterialIcon(Icons.Outlined.Info) },
                onClick = { navigator.push(Route.About) },
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun MaterialWarningCard(text: String) {
    MaterialSurfaceCard(
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentPadding = PaddingValues(16.dp),
    ) {
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

@Composable
private fun MaterialInfoRow(title: String, content: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MaterialUpdateDialog(
    show: Boolean,
    updateResult: UpdateResult,
    onDismissRequest: () -> Unit,
) {
    if (!show) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var dontRemind by remember(updateResult.versionName) { mutableStateOf(false) }
    val showDownloadDialog = remember { mutableStateOf(false) }
    val downloadProgress = remember { mutableIntStateOf(0) }

    fun dismiss() {
        if (dontRemind) {
            UpdateChecker.markSkipped(context, updateResult.versionName)
        }
        onDismissRequest()
    }

    DownloadProgressDialog(
        show = showDownloadDialog.value,
        progress = downloadProgress.intValue
    )

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = stringResource(R.string.update_available))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = stringResource(R.string.update_new_version, updateResult.versionName))
                if (!updateResult.changelog.isNullOrBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.update_changelog),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = updateResult.changelog,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { dontRemind = !dontRemind },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Checkbox(
                        checked = dontRemind,
                        onCheckedChange = { dontRemind = it },
                    )
                    Text(text = stringResource(R.string.update_dont_remind))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    dismiss()
                    showDownloadDialog.value = true
                    scope.launch {
                        ApkInstaller.downloadAndInstall(
                            context = context,
                            url = updateResult.downloadUrl,
                            onProgress = { progress ->
                                downloadProgress.intValue = progress
                            },
                            onComplete = {
                                showDownloadDialog.value = false
                            },
                            onError = {
                                showDownloadDialog.value = false
                            }
                        )
                    }
                },
            ) {
                Text(text = stringResource(R.string.update_now))
            }
        },
        dismissButton = {
            TextButton(onClick = ::dismiss) {
                Text(text = stringResource(R.string.update_later))
            }
        },
    )
}
