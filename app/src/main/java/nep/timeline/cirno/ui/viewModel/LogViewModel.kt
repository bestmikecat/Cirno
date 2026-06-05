package nep.timeline.cirno.ui.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import nep.timeline.cirno.ui.utils.RootLogRepository

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
    val pendingScrollToBottom: Boolean = false,
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
        if (snapshot.size < logFileSize) {
            val fullSnapshot = RootLogRepository.getLogSnapshot()
            logFileSize = fullSnapshot.size
            _uiState.update {
                it.copy(
                    allLines = fullSnapshot.lines,
                    isTruncated = fullSnapshot.truncated,
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

    fun requestScrollToBottom() {
        _uiState.update { it.copy(pendingScrollToBottom = true) }
    }

    fun cancelScrollToBottom() {
        _uiState.update { it.copy(pendingScrollToBottom = false) }
    }

    private fun limitLines(lines: List<String>): List<String> {
        return if (lines.size > MAX_LOG_LINES) lines.takeLast(MAX_LOG_LINES) else lines
    }

    private companion object {
        const val LOG_REFRESH_INTERVAL_MS = 1_000L
        const val MAX_LOG_LINES = 10_000
    }
}
