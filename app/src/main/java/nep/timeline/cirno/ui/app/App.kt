package nep.timeline.cirno.ui.app

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import nep.timeline.cirno.GlobalVars

@Composable
fun App(
    active: Boolean,
    padding: PaddingValues = PaddingValues(0.dp),
    configLoadKey: Any? = null,
) {
    val appStateHolder = remember(configLoadKey) {
        val settings = GlobalVars.globalSettings
        mutableStateOf(
            if (settings != null) {
                AppState(
                    uiStyle = settings.uiStyle,
                    navigationStyle = settings.navigationStyle,
                    colorMode = settings.colorMode,
                    blur = settings.blurUI,
                )
            } else {
                AppState()
            }
        )
    }
    val appState = appStateHolder.value
    val updateAppState: ((AppState) -> AppState) -> Unit = remember {
        { transform -> appStateHolder.value = transform(appStateHolder.value) }
    }

    AppTheme(
        uiStyle = appState.uiStyle,
        colorMode = appState.colorMode,
        smoothRounding = false,
    ) {
        CompositionLocalProvider(
            LocalAppState provides appState,
            LocalUpdateAppState provides updateAppState,
        ) {
            key(appState.uiStyle) {
                if (appState.uiStyle == UI_STYLE_MATERIAL) {
                    MaterialAppContent(active, padding)
                } else {
                    AppContent(active, padding)
                }
            }
        }
    }
}
