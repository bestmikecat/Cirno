package nep.timeline.cirno.screen

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun rememberHazeStyle(): HazeStyle {
    val colorScheme = MiuixTheme.colorScheme
    return HazeStyle(
        backgroundColor = colorScheme.surface,
        tint = HazeTint(colorScheme.surface.copy(0.67f))
    )
}

@Composable
fun HazeScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable (hazeState: HazeState, hazeStyle: HazeStyle) -> Unit = { _, _ -> },
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val hazeState = rememberHazeState()
    val hazeStyle = rememberHazeStyle()

    Scaffold(
        topBar = { topBar(hazeState, hazeStyle) },
        bottomBar = bottomBar,
        modifier = modifier
    ) { padding ->
        Surface(
            modifier = Modifier
                .hazeSource(state = hazeState)
                .fillMaxSize(),
            color = MiuixTheme.colorScheme.surface
        ) {
            content(padding)
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    SmallTitle(text = text, modifier = modifier)
}

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    topPadding: Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(top = topPadding),
        content = content
    )
}
