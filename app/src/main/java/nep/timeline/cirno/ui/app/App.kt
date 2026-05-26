package nep.timeline.cirno.ui.app

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import nep.timeline.cirno.GlobalVars

@Composable
fun App(
    active: Boolean,
    padding: PaddingValues = PaddingValues(0.dp),
    configLoadKey: Any? = null,
) {
    var appState by remember(configLoadKey) {
        val settings = GlobalVars.globalSettings
        mutableStateOf(
            if (settings != null) {
                AppState(
                    navigationStyle = settings.navigationStyle,
                    colorMode = settings.colorMode,
                    blur = settings.blurUI,
                )
            } else {
                AppState()
            }
        )
    }
    val updateAppState: ((AppState) -> AppState) -> Unit = remember {
        { transform -> appState = transform(appState) }
    }

    AppTheme(
        colorMode = appState.colorMode,
        smoothRounding = false,
    ) {
        CompositionLocalProvider(
            LocalAppState provides appState,
            LocalUpdateAppState provides updateAppState,
        ) {
            AppContent(active, padding)
        }
    }
}
