@file:OptIn(ExperimentalScrollBarApi::class)
package nep.timeline.cirno.ui.page

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import nep.timeline.cirno.R
import nep.timeline.cirno.ui.app.LocalIsWideScreen
import nep.timeline.cirno.ui.app.LocalNavigator
import nep.timeline.cirno.ui.custom.BackNavigationIcon
import nep.timeline.cirno.ui.utils.ConfigBinderRepository
import nep.timeline.cirno.ui.utils.pageContentPadding
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.VerticalScrollBar
import top.yukonga.miuix.kmp.basic.rememberScrollBarAdapter
import top.yukonga.miuix.kmp.interfaces.ExperimentalScrollBarApi
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

@Composable
fun LogPage(
    padding: PaddingValues,
) {
    val topAppBarScrollBehavior = MiuixScrollBehavior()
    val navigator = LocalNavigator.current

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = stringResource(R.string.logs_title),
                scrollBehavior = topAppBarScrollBehavior,
                color = colorScheme.surface,
                defaultWindowInsetsPadding = false,
                navigationIcon = {
                    BackNavigationIcon(
                        onClick = { navigator.pop() },
                    )
                },
            )
        },
    ) { innerPadding ->
        LogContent(
            padding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding(),
            ),
            topAppBarScrollBehavior = topAppBarScrollBehavior,
        )
    }
}

@Composable
private fun LogContent(
    padding: PaddingValues,
    topAppBarScrollBehavior: top.yukonga.miuix.kmp.basic.ScrollBehavior,
) {
    val isWideScreen = LocalIsWideScreen.current
    val lazyListState = rememberLazyListState()
    val contentPadding = pageContentPadding(padding, padding, isWideScreen)

    var logContent by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            logContent = ConfigBinderRepository.getLogContent()
            delay(3000)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val lines = logContent?.lines()?.filter { it.isNotBlank() } ?: emptyList()

        if (logContent == null || lines.isEmpty()) {
            Text(
                text = stringResource(R.string.logs_empty),
                modifier = Modifier.align(Alignment.Center),
                fontSize = 16.sp,
                color = colorScheme.onSurfaceVariantSummary,
            )
        } else {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding,
            ) {
                items(lines) { line ->
                    Text(
                        text = line,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp),
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        color = when {
                            line.contains("错误") || line.contains("ERROR") -> colorScheme.error
                            line.contains("警告") || line.contains("WARN") -> colorScheme.primary
                            else -> colorScheme.onSurface
                        },
                    )
                }
            }

            VerticalScrollBar(
                adapter = rememberScrollBarAdapter(lazyListState),
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                trackPadding = contentPadding,
            )
        }
    }
}
