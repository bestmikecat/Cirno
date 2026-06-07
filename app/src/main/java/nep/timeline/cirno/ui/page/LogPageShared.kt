package nep.timeline.cirno.ui.page

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nep.timeline.cirno.ui.viewModel.LogDisplayLevel
import nep.timeline.cirno.ui.viewModel.LogUiState
import nep.timeline.cirno.ui.viewModel.LogViewModel

data class LogScreenState(
    val uiState: LogUiState,
    val filteredLines: List<String>,
)

@Composable
fun rememberLogScreenState(logViewModel: LogViewModel): LogScreenState {
    val uiState by logViewModel.uiState.collectAsStateWithLifecycle()
    val filteredLines by remember(uiState.allLines, uiState.searchQuery, uiState.selectedLevel) {
        derivedStateOf {
            uiState.allLines.filter {
                it.matchesLogLevel(uiState.selectedLevel) &&
                    (uiState.searchQuery.isBlank() || it.contains(uiState.searchQuery, ignoreCase = true))
            }
        }
    }

    DisposableEffect(Unit) {
        logViewModel.startLogSession()
        onDispose { logViewModel.stopLogSession() }
    }

    return LogScreenState(uiState = uiState, filteredLines = filteredLines)
}

fun String.matchesLogLevel(level: LogDisplayLevel): Boolean {
    return when (level) {
        LogDisplayLevel.All -> true
        LogDisplayLevel.Debug -> contains("调试") || contains("DEBUG")
        LogDisplayLevel.Info -> contains("信息") || contains("INFO")
        LogDisplayLevel.Warning -> contains("警告") || contains("WARN")
        LogDisplayLevel.Error -> contains("错误") || contains("ERROR")
    }
}
