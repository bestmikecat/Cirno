package nep.timeline.cirno.ui.app

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

const val UI_STYLE_MIUIX = 0
const val UI_STYLE_MATERIAL = 1

val LocalColorMode = compositionLocalOf { 0 }
val LocalUiStyle = compositionLocalOf { UI_STYLE_MIUIX }

@Composable
fun AppTheme(
    uiStyle: Int = UI_STYLE_MIUIX,
    colorMode: Int = 0,
    keyColor: Color? = null,
    paletteStyle: Int = 0,
    colorSpec: Int = 0,
    smoothRounding: Boolean = true,
    content: @Composable () -> Unit,
) {
    val spec = ThemeColorSpec.entries.getOrNull(colorSpec) ?: ThemeColorSpec.Spec2021
    val style = ThemePaletteStyle.entries.getOrNull(paletteStyle) ?: ThemePaletteStyle.Content
    val context = LocalContext.current
    val controller = remember(colorMode, keyColor, spec, style) {
        when (colorMode) {
            1 -> ThemeController(ColorSchemeMode.Light)
            2 -> ThemeController(ColorSchemeMode.Dark)
            3 -> ThemeController(ColorSchemeMode.MonetSystem, keyColor = keyColor, colorSpec = spec, paletteStyle = style)
            4 -> ThemeController(ColorSchemeMode.MonetLight, keyColor = keyColor, colorSpec = spec, paletteStyle = style)
            5 -> ThemeController(ColorSchemeMode.MonetDark, keyColor = keyColor, colorSpec = spec, paletteStyle = style)
            else -> ThemeController(ColorSchemeMode.System)
        }
    }
    val useDarkMaterial = when (colorMode) {
        2, 5 -> true
        1, 4 -> false
        else -> isSystemInDarkTheme()
    }
    val materialColors = remember(colorMode, useDarkMaterial, context) {
        val useDynamic = colorMode in 3..5 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        when {
            useDynamic && useDarkMaterial -> dynamicDarkColorScheme(context)
            useDynamic -> dynamicLightColorScheme(context)
            useDarkMaterial -> darkColorScheme()
            else -> lightColorScheme()
        }
    }
    val materialShapes = remember {
        Shapes(
            extraSmall = RoundedCornerShape(12.dp),
            small = RoundedCornerShape(18.dp),
            medium = RoundedCornerShape(24.dp),
            large = RoundedCornerShape(28.dp),
            extraLarge = RoundedCornerShape(32.dp),
        )
    }
    CompositionLocalProvider(
        LocalColorMode provides colorMode,
        LocalUiStyle provides uiStyle,
    ) {
        MiuixTheme(
            controller = controller,
            smoothRounding = smoothRounding,
        ) {
            MaterialTheme(
                colorScheme = materialColors,
                shapes = materialShapes,
                typography = Typography(),
                content = content,
            )
        }
    }
}

@Composable
fun isInDarkTheme(): Boolean = when (LocalColorMode.current) {
    1, 4 -> false

    // Force dark mode
    2, 5, 6 -> true

    // Follow system (0 or default)
    else -> isSystemInDarkTheme()
}

val KeyColors: List<Pair<String, Color>> = listOf(
    "Blue" to Color(0xFF3482FF),
    "Green" to Color(0xFF36D167),
    "Purple" to Color(0xFF7C4DFF),
    "Yellow" to Color(0xFFFFB21D),
    "Orange" to Color(0xFFFF5722),
    "Pink" to Color(0xFFE91E63),
    "Teal" to Color(0xFF00BCD4),
)

fun keyColorFor(index: Int): Color? = if (index <= 0) null else KeyColors.getOrNull(index - 1)?.second
