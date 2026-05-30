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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import nep.timeline.cirno.MainActivity.LogViewModelSingleton.logViewModel
import nep.timeline.cirno.R
import nep.timeline.cirno.ui.app.LocalIsWideScreen
import nep.timeline.cirno.ui.app.LocalNavigator
import nep.timeline.cirno.ui.custom.BackNavigationIcon
import nep.timeline.cirno.ui.utils.pageContentPadding
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.ListPopupDefaults
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SearchBar
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.VerticalScrollBar
import top.yukonga.miuix.kmp.basic.rememberScrollBarAdapter
import top.yukonga.miuix.kmp.interfaces.ExperimentalScrollBarApi
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Search
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.window.WindowListPopup

@Composable
fun LogPage(
    padding: PaddingValues,
) {
    val topAppBarScrollBehavior = MiuixScrollBehavior()
    val navigator = LocalNavigator.current
    val lazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val uiState by logViewModel.uiState.collectAsStateWithLifecycle()

    val filteredLines by remember(uiState.loadedLines, uiState.searchQuery) {
        derivedStateOf {
            if (uiState.searchQuery.isBlank()) uiState.loadedLines
            else uiState.loadedLines.filter { it.contains(uiState.searchQuery, ignoreCase = true) }
        }
    }

    LaunchedEffect(Unit) {
        logViewModel.ensureInitialized()
    }

    LaunchedEffect(lazyListState, uiState.hasMore, uiState.isLoadingMore) {
        snapshotFlow {
            val lastVisible = lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val totalItems = lazyListState.layoutInfo.totalItemsCount
            lastVisible to totalItems
        }.distinctUntilChanged().collect { (lastVisible, totalItems) ->
            if (!uiState.hasMore || uiState.isLoadingMore || totalItems <= 0) {
                return@collect
            }
            if (lastVisible >= totalItems - 5) {
                logViewModel.loadNextPage()
            }
        }
    }

    LaunchedEffect(uiState.pendingScrollToBottom, uiState.loadedLines.size, uiState.hasMore, uiState.isLoadingMore, uiState.searchQuery) {
        if (!uiState.pendingScrollToBottom) {
            return@LaunchedEffect
        }

        val bottomIndex = if (filteredLines.isEmpty()) 0 else filteredLines.size
        lazyListState.scrollToItem(bottomIndex)

        if (uiState.searchQuery.isNotBlank()) {
            logViewModel.cancelScrollToBottom()
            return@LaunchedEffect
        }

        if (uiState.hasMore && !uiState.isLoadingMore) {
            logViewModel.loadNextPage()
            return@LaunchedEffect
        }

        if (!uiState.hasMore) {
            logViewModel.cancelScrollToBottom()
        }
    }

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
                    LogTopBarActions(
                        onScrollTop = {
                            scope.launch {
                                logViewModel.cancelScrollToBottom()
                                lazyListState.animateScrollToItem(0)
                            }
                        },
                        onScrollBottom = {
                            scope.launch {
                                logViewModel.requestScrollToBottom()
                                val bottomIndex = if (filteredLines.isEmpty()) 0 else filteredLines.size
                                lazyListState.scrollToItem(bottomIndex)
                            }
                        }
                    )
                }
            )
        },
    ) { innerPadding ->
        LogContent(
            padding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding(),
            ),
            lazyListState = lazyListState,
            lines = filteredLines,
            hasAnyLog = uiState.loadedLines.isNotEmpty(),
            isInitialLoadDone = uiState.isInitialLoadDone,
            searchQuery = uiState.searchQuery,
            onSearchQueryChange = logViewModel::updateSearchQuery,
            searchExpanded = uiState.searchExpanded,
            onSearchExpandedChange = logViewModel::setSearchExpanded,
        )
    }
}

@Composable
private fun LogTopBarActions(
    onScrollTop: () -> Unit,
    onScrollBottom: () -> Unit,
) {
    val showPopup = remember { mutableStateOf(false) }
    val holdDownState = remember { mutableStateOf(false) }
    val hapticFeedback = LocalHapticFeedback.current

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
            val dismissState = top.yukonga.miuix.kmp.theme.LocalDismissState.current
            val items = listOf(
                stringResource(R.string.scroll_to_top),
                stringResource(R.string.scroll_to_bottom),
            )
            ListPopupColumn {
                items.forEachIndexed { index, string ->
                    key(index) {
                        DropdownImpl(
                            text = string,
                            optionSize = items.size,
                            isSelected = false,
                            onSelectedIndexChange = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                                when (index) {
                                    0 -> onScrollTop()
                                    1 -> onScrollBottom()
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
    lazyListState: LazyListState,
    lines: List<String>,
    hasAnyLog: Boolean,
    isInitialLoadDone: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchExpanded: Boolean,
    onSearchExpandedChange: (Boolean) -> Unit,
) {
    val isWideScreen = LocalIsWideScreen.current
    val contentPadding = pageContentPadding(padding, padding, isWideScreen)
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    Box(modifier = Modifier.fillMaxSize()) {
        if (isInitialLoadDone && !hasAnyLog) {
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
                                    onQueryChange = onSearchQueryChange,
                                    onSearch = { keyboardController?.hide() },
                                    expanded = searchExpanded,
                                    onExpandedChange = onSearchExpandedChange,
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
                            onExpandedChange = onSearchExpandedChange
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
