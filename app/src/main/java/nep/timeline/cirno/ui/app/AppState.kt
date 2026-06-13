package nep.timeline.cirno.ui.app

import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import top.yukonga.miuix.kmp.blur.isRenderEffectSupported

@Stable
data class AppState(
    // UI
    val uiStyle: Int = 0,
    val navigationStyle: Int = 0,
    val colorMode: Int = 0,
    val themeKeyColor: Int = 0,
    val themeColorSpec: Int = 0,
    val themePaletteStyle: Int = 0,
    val blur: Boolean = isRenderEffectSupported(),
    val enablePageUserScroll: Boolean = false,
    val enableScrollEndHaptic: Boolean = true,
    val enableCornerClip: Boolean = true,
    val enableDim: Boolean = true,
    val blockInputDuringTransition: Boolean = true,
    val popDirectionFollowsSwipeEdge: Boolean = false,
)

val LocalAppState = compositionLocalOf<AppState> {
    error("No AppState provided!")
}

val LocalUpdateAppState = staticCompositionLocalOf<((AppState) -> AppState) -> Unit> {
    error("No AppState updater provided!")
}
