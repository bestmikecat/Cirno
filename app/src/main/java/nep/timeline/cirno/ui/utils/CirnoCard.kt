package nep.timeline.cirno.ui.utils

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import nep.timeline.cirno.ui.app.LocalAppState
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardColors
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.isRenderEffectSupported
import top.yukonga.miuix.kmp.shapes.SmoothRoundedCornerShape
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

@Composable
fun CirnoCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    insideMargin: PaddingValues = PaddingValues(0.dp),
    colors: CardColors = CardDefaults.defaultColors(),
    pressFeedbackType: PressFeedbackType = PressFeedbackType.Tilt,
    showIndication: Boolean = true,
    onClick: (() -> Unit)? = null,
    backdrop: LayerBackdrop? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val hasBackground = BackgroundManager.currentUri != null
    val blurActive = hasBackground && LocalAppState.current.blur && backdrop != null && isRenderEffectSupported()
    val density = LocalDensity.current
    val cardColors = if (hasBackground) {
        colors.copy(color = colors.color.copy(alpha = BackgroundManager.cardAlpha))
    } else {
        colors
    }
    val cardModifier = if (blurActive) {
        modifier.then(
            Modifier.textureBlur(
                backdrop = backdrop,
                shape = SmoothRoundedCornerShape(cornerRadius),
                blurRadius = with(density) { BackgroundManager.cardBlurRadius.toPx() },
                colors = BlurColors(
                    blendColors = listOf(
                        BlendColorEntry(color = MiuixTheme.colorScheme.surface.copy(alpha = BackgroundManager.cardAlpha)),
                    ),
                ),
            )
        )
    } else {
        modifier
    }

    if (onClick == null) {
        Card(
            modifier = cardModifier,
            cornerRadius = cornerRadius,
            insideMargin = insideMargin,
            colors = cardColors,
            content = content,
        )
    } else {
        Card(
            modifier = cardModifier,
            cornerRadius = cornerRadius,
            insideMargin = insideMargin,
            colors = cardColors,
            pressFeedbackType = pressFeedbackType,
            showIndication = showIndication,
            onClick = onClick,
            content = content,
        )
    }
}

@Composable
fun Modifier.cirnoCardBackground(
    backdrop: LayerBackdrop?,
    shape: Shape,
    color: Color = MiuixTheme.colorScheme.surfaceContainer,
): Modifier {
    val hasBackground = BackgroundManager.currentUri != null
    val blurActive = hasBackground && LocalAppState.current.blur && backdrop != null && isRenderEffectSupported()
    val backgroundColor = if (hasBackground) color.copy(alpha = BackgroundManager.cardAlpha) else color
    val density = LocalDensity.current
    val blurredModifier = if (blurActive) {
        then(
            Modifier.textureBlur(
                backdrop = backdrop,
                shape = shape,
                blurRadius = with(density) { BackgroundManager.cardBlurRadius.toPx() },
                colors = BlurColors(
                    blendColors = listOf(
                        BlendColorEntry(color = MiuixTheme.colorScheme.surface.copy(alpha = BackgroundManager.cardAlpha)),
                    ),
                ),
            )
        )
    } else {
        this
    }
    return blurredModifier.background(backgroundColor, shape)
}
