package nep.timeline.cirno.ui.viewModel

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.topjohnwu.superuser.io.SuFile
import com.topjohnwu.superuser.io.SuFileInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nep.timeline.cirno.GlobalVars
import nep.timeline.cirno.R
import nep.timeline.cirno.configs.settings.GlobalSettings
import nep.timeline.cirno.ui.utils.AppContext
import nep.timeline.cirno.ui.utils.LogEmptyReason
import nep.timeline.cirno.ui.utils.RootLogRepository
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LogDisplayLevel {
    All,
    Debug,
    Info,
    Warning,
    Error,
}

data class LogUiState(
    val allLines: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val isInitialLoadDone: Boolean = false,
    val isTruncated: Boolean = false,
    val searchQuery: String = "",
    val searchExpanded: Boolean = false,
    val selectedLevel: LogDisplayLevel = LogDisplayLevel.All,
    val emptyReason: LogEmptyReason? = null,
    val emptyReasonDetail: String? = null,
)

class LogViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(LogUiState())
    val uiState: StateFlow<LogUiState> = _uiState

    private var logFileSize = 0L
    private var refreshJob: Job? = null
    private var loadJob: Job? = null

    fun startLogSession() {
        if (!_uiState.value.isInitialLoadDone && loadJob?.isActive != true) {
            loadAllLogs()
        }
        startRefresh()
    }

    fun stopLogSession() {
        refreshJob?.cancel()
        refreshJob = null
        loadJob?.cancel()
        loadJob = null
        logFileSize = 0L
        _uiState.update {
            LogUiState(
                searchQuery = it.searchQuery,
                searchExpanded = it.searchExpanded,
                selectedLevel = it.selectedLevel,
            )
        }
    }

    private fun loadAllLogs() {
        _uiState.update { it.copy(isLoading = true) }
        loadJob = viewModelScope.launch(Dispatchers.IO) {
            val snapshot = RootLogRepository.getLogSnapshot()
            logFileSize = snapshot.size
            _uiState.update {
                it.copy(
                    allLines = snapshot.lines,
                    isLoading = false,
                    isInitialLoadDone = true,
                    isTruncated = snapshot.truncated,
                    emptyReason = snapshot.emptyReason,
                    emptyReasonDetail = snapshot.emptyReasonDetail,
                )
            }
        }
    }

    private fun startRefresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(LOG_REFRESH_INTERVAL_MS)
                refreshIncremental()
            }
        }
    }

    private fun refreshIncremental() {
        if (!_uiState.value.isInitialLoadDone) return
        val snapshot = RootLogRepository.getLogContentAfter(logFileSize)
        if (snapshot.emptyReason != null) {
            _uiState.update {
                it.copy(
                    emptyReason = snapshot.emptyReason,
                    emptyReasonDetail = snapshot.emptyReasonDetail,
                )
            }
        }
        if (_uiState.value.allLines.isEmpty() && snapshot.lines.isEmpty()) {
            _uiState.update {
                it.copy(
                    emptyReason = snapshot.emptyReason,
                    emptyReasonDetail = snapshot.emptyReasonDetail,
                )
            }
        }
        if (snapshot.size < logFileSize) {
            val fullSnapshot = RootLogRepository.getLogSnapshot()
            logFileSize = fullSnapshot.size
            _uiState.update {
                it.copy(
                    allLines = fullSnapshot.lines,
                    isTruncated = fullSnapshot.truncated,
                    emptyReason = fullSnapshot.emptyReason,
                    emptyReasonDetail = fullSnapshot.emptyReasonDetail,
                )
            }
            return
        }

        logFileSize = snapshot.size
        if (snapshot.lines.isEmpty()) return

        _uiState.update {
            val mergedLines = limitLines(it.allLines + snapshot.lines)
            it.copy(
                allLines = mergedLines,
                isTruncated = it.isTruncated || mergedLines.size < it.allLines.size + snapshot.lines.size,
                emptyReason = null,
                emptyReasonDetail = null,
            )
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun selectLevel(level: LogDisplayLevel) {
        _uiState.update { it.copy(selectedLevel = level) }
    }

    fun setSearchExpanded(expanded: Boolean) {
        _uiState.update { it.copy(searchExpanded = expanded) }
    }

    fun exportLog(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 检查日志级别
                val logLevel = GlobalVars.globalSettings?.logLevel
                if (logLevel != GlobalSettings.LOG_LEVEL_DEBUG) {
                    AppContext.showToast(R.string.export_log_requires_debug)
                    return@launch
                }

                // 读取源文件
                val sourceFile = SuFile(GlobalVars.LOG_DIR, "current.log")
                if (!sourceFile.exists() || sourceFile.length() == 0L) {
                    AppContext.showToast("日志文件为空或不存在")
                    return@launch
                }

                // 清理旧缓存
                val cacheDir = context.cacheDir
                cacheDir.listFiles()?.filter { it.name.startsWith("cirno_log_") }?.forEach { it.delete() }

                // 生成带时间戳的文件名
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val destFile = File(cacheDir, "cirno_log_$timestamp.log")

                // 复制文件
                SuFileInputStream.open(sourceFile).use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                // 触发分享
                withContext(Dispatchers.Main) {
                    val uri = FileProvider.getUriForFile(
                        context,
                        "nep.timeline.cirno.fileprovider",
                        destFile
                    )
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    val chooser = Intent.createChooser(shareIntent, "分享日志")
                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(chooser)
                }
            } catch (e: Exception) {
                val msg = e.message ?: e::class.simpleName
                AppContext.showToast(context.getString(R.string.export_log_failed, msg ?: "未知错误"))
            }
        }
    }

    private fun limitLines(lines: List<String>): List<String> {
        return if (lines.size > MAX_LOG_LINES) lines.takeLast(MAX_LOG_LINES) else lines
    }

    private companion object {
        const val LOG_REFRESH_INTERVAL_MS = 1_000L
        const val MAX_LOG_LINES = 10_000
    }
}
