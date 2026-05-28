package nep.timeline.cirno.ui.viewModel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import nep.timeline.cirno.GlobalVars
import nep.timeline.cirno.ui.app.AppState

class AppUiStateViewModel : ViewModel() {
    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    fun loadFromGlobalSettings() {
        val settings = GlobalVars.globalSettings
        _state.value = if (settings != null) {
            AppState(
                uiStyle = settings.uiStyle,
                navigationStyle = settings.navigationStyle,
                colorMode = settings.colorMode,
                blur = settings.blurUI,
            )
        } else {
            AppState()
        }
    }

    fun update(transform: (AppState) -> AppState) {
        _state.value = transform(_state.value)
    }
}
