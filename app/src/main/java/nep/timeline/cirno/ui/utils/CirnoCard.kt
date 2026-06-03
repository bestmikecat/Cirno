package nep.timeline.cirno.ui.utils

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardColors
import top.yukonga.miuix.kmp.basic.CardDefaults
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
    content: @Composable ColumnScope.() -> Unit,
) {
    val hasBackground = BackgroundManager.currentUri != null
    val cardColors = if (hasBackground) {
        colors.copy(color = colors.color.copy(alpha = BackgroundManager.cardAlpha))
    } else {
        colors
    }

    if (onClick == null) {
        Card(
            modifier = modifier,
            cornerRadius = cornerRadius,
            insideMargin = insideMargin,
            colors = cardColors,
            content = content,
        )
    } else {
        Card(
            modifier = modifier,
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
    shape: Shape,
    color: Color = MiuixTheme.colorScheme.surfaceContainer,
): Modifier {
    val hasBackground = BackgroundManager.currentUri != null
    val backgroundColor = if (hasBackground) color.copy(alpha = BackgroundManager.cardAlpha) else color
    return background(backgroundColor, shape)
}
