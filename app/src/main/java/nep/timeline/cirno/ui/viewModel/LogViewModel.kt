package nep.timeline.cirno.ui.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import nep.timeline.cirno.ui.utils.RootLogRepository

data class LogUiState(
    val loadedLines: List<String> = emptyList(),
    val nextStartLine: Int = 0,
    val hasMore: Boolean = true,
    val isLoadingMore: Boolean = false,
    val isInitialLoadDone: Boolean = false,
    val searchQuery: String = "",
    val searchExpanded: Boolean = false,
    val pendingScrollToBottom: Boolean = false,
)

class LogViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(LogUiState())
    val uiState: StateFlow<LogUiState> = _uiState

    fun ensureInitialized() {
        if (_uiState.value.isInitialLoadDone || _uiState.value.isLoadingMore) {
            return
        }
        loadNextPage()
    }

    fun loadNextPage() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMore) {
            return
        }

        _uiState.update { it.copy(isLoadingMore = true) }
        viewModelScope.launch(Dispatchers.IO) {
            val currentState = _uiState.value
            val page = RootLogRepository.getLogContentPage(currentState.nextStartLine, LOG_PAGE_SIZE)
            _uiState.update {
                val mergedLines = if (page.isEmpty()) it.loadedLines else it.loadedLines + page
                it.copy(
                    loadedLines = mergedLines,
                    nextStartLine = it.nextStartLine + page.size,
                    hasMore = page.size == LOG_PAGE_SIZE,
                    isLoadingMore = false,
                    isInitialLoadDone = true,
                )
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
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

    private companion object {
        const val LOG_PAGE_SIZE = 200
    }
}
