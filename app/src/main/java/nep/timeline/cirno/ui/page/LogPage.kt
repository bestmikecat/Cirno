@file:OptIn(ExperimentalScrollBarApi::class)
package nep.timeline.cirno.ui.page

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nep.timeline.cirno.R
import nep.timeline.cirno.ui.app.LocalIsWideScreen
import nep.timeline.cirno.ui.app.LocalNavigator
import nep.timeline.cirno.ui.custom.BackNavigationIcon
import nep.timeline.cirno.ui.utils.ConfigBinderRepository
import nep.timeline.cirno.ui.utils.pageContentPadding
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.ListPopupDefaults
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SearchBar
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.VerticalScrollBar
import top.yukonga.miuix.kmp.basic.rememberScrollBarAdapter
import top.yukonga.miuix.kmp.interfaces.ExperimentalScrollBarApi
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Search
import top.yukonga.miuix.kmp.popup.PopupPositionProvider
import top.yukonga.miuix.kmp.theme.LocalDismissState
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.window.WindowListPopup

@Composable
fun LogPage(
    padding: PaddingValues,
) {
    val topAppBarScrollBehavior = MiuixScrollBehavior()
    val navigator = LocalNavigator.current
    val lazyListState = rememberLazyListState()

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
                actions = {
                    LogTopBarActions(lazyListState = lazyListState)
                }
            )
        },
    ) { innerPadding ->
        LogContent(
            padding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding(),
            ),
            topAppBarScrollBehavior = topAppBarScrollBehavior,
            lazyListState = lazyListState,
        )
    }
}

@Composable
private fun LogTopBarActions(
    lazyListState: androidx.compose.foundation.lazy.LazyListState,
) {
    val showPopup = remember { mutableStateOf(false) }
    val holdDownState = remember { mutableStateOf(false) }
    val hapticFeedback = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val dismissState = LocalDismissState.current

    IconButton(
        onClick = {
            showPopup.value = true
            holdDownState.value = true
        },
        holdDownState = holdDownState.value,
    ) {
        Icon(
            imageVector = MiuixIcons.More,
            contentDescription = null,
            tint = colorScheme.onBackground,
        )
    }
    WindowListPopup(
        show = showPopup.value,
        popupPositionProvider = ListPopupDefaults.ContextMenuPositionProvider,
        alignment = PopupPositionProvider.Align.TopEnd,
        onDismissRequest = { showPopup.value = false },
        onDismissFinished = { holdDownState.value = false },
        content = {
            val items = listOf(
                stringResource(R.string.scroll_to_top),
                stringResource(R.string.scroll_to_bottom),
            )
            ListPopupColumn {
                items.forEachIndexed { index, string ->
                    androidx.compose.runtime.key(index) {
                        DropdownImpl(
                            text = string,
                            optionSize = items.size,
                            isSelected = false,
                            onSelectedIndexChange = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                                scope.launch {
                                    when (index) {
                                        0 -> lazyListState.animateScrollToItem(0)
                                        1 -> lazyListState.animateScrollToItem(lazyListState.layoutInfo.totalItemsCount - 1)
                                    }
                                }
                                dismissState?.invoke()
                            },
                            index = index
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun LogContent(
    padding: PaddingValues,
    topAppBarScrollBehavior: top.yukonga.miuix.kmp.basic.ScrollBehavior,
    lazyListState: androidx.compose.foundation.lazy.LazyListState,
) {
    val isWideScreen = LocalIsWideScreen.current
    val contentPadding = pageContentPadding(padding, padding, isWideScreen)
    val focusManager = LocalFocusManager.current
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    var logContent by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var searchExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            logContent = ConfigBinderRepository.getLogContent()
            delay(3000)
        }
    }

    val allLines = logContent?.lines()?.filter { it.isNotBlank() } ?: emptyList()
    val lines = if (searchQuery.isBlank()) allLines
        else allLines.filter { it.contains(searchQuery, ignoreCase = true) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (logContent == null || allLines.isEmpty()) {
            Text(
                text = stringResource(R.string.logs_empty),
                modifier = Modifier.align(Alignment.Center),
                fontSize = 16.sp,
                color = colorScheme.onSurfaceVariantSummary,
            )
        } else {
            SelectionContainer {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = contentPadding,
                ) {
                    item(key = "logSearch") {
                        SearchBar(
                            modifier = Modifier.padding(
                                start = 12.dp, end = 12.dp, top = 12.dp, bottom = 6.dp
                            ),
                            inputField = {
                                InputField(
                                    query = searchQuery,
                                    onQueryChange = { searchQuery = it },
                                    onSearch = { keyboardController?.hide() },
                                    expanded = searchExpanded,
                                    onExpandedChange = { searchExpanded = it },
                                    label = stringResource(R.string.search),
                                    leadingIcon = {
                                        Icon(
                                            modifier = Modifier
                                                .padding(start = 12.dp, end = 8.dp)
                                                .size(20.dp)
                                                .alpha(0.4f),
                                            imageVector = MiuixIcons.Basic.Search,
                                            tint = colorScheme.onSurfaceContainer,
                                            contentDescription = null
                                        )
                                    }
                                )
                            },
                            expanded = searchExpanded,
                            onExpandedChange = { searchExpanded = it }
                        ) {}
                    }
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
            }

            VerticalScrollBar(
                adapter = rememberScrollBarAdapter(lazyListState),
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                trackPadding = contentPadding,
            )
        }
    }
}
