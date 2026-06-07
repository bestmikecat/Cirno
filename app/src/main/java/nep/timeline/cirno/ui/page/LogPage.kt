@file:OptIn(ExperimentalScrollBarApi::class)
package nep.timeline.cirno.ui.page

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import nep.timeline.cirno.MainActivity.LogViewModelSingleton.logViewModel
import nep.timeline.cirno.R
import nep.timeline.cirno.ui.app.LocalIsWideScreen
import nep.timeline.cirno.ui.app.LocalNavigator
import nep.timeline.cirno.ui.custom.BackNavigationIcon
import nep.timeline.cirno.ui.utils.pageContentPadding
import nep.timeline.cirno.ui.utils.LogEmptyReason
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
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
import nep.timeline.cirno.ui.viewModel.LogDisplayLevel

@Composable
fun LogPage(
    padding: PaddingValues,
) {
    val topAppBarScrollBehavior = MiuixScrollBehavior()
    val navigator = LocalNavigator.current
    val lazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val uiState by logViewModel.uiState.collectAsStateWithLifecycle()

    val filteredLines by remember(uiState.allLines, uiState.searchQuery, uiState.selectedLevel) {
        derivedStateOf {
            uiState.allLines.filter {
                it.matchesLogLevel(uiState.selectedLevel) &&
                    (uiState.searchQuery.isBlank() || it.contains(uiState.searchQuery, ignoreCase = true))
            }
        }
    }

    DisposableEffect(Unit) {
        logViewModel.startLogSession()
        onDispose { logViewModel.stopLogSession() }
    }

    Scaffold(
        containerColor = colorScheme.surface,
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
                        selectedLevel = uiState.selectedLevel,
                        onLevelSelected = logViewModel::selectLevel,
                        onScrollTop = {
                            scope.launch {
                                lazyListState.animateScrollToItem(0)
                            }
                        },
                        onScrollBottom = {
                            scope.launch {
                                val bottomIndex = if (filteredLines.isEmpty()) 0 else lazyListState.layoutInfo.totalItemsCount.minus(1).coerceAtLeast(0)
                                lazyListState.animateScrollToItem(bottomIndex)
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
            hasAnyLog = uiState.allLines.isNotEmpty(),
            isLoading = uiState.isLoading,
            isInitialLoadDone = uiState.isInitialLoadDone,
            isTruncated = uiState.isTruncated,
            emptyReason = uiState.emptyReason,
            emptyReasonDetail = uiState.emptyReasonDetail,
            searchQuery = uiState.searchQuery,
            onSearchQueryChange = logViewModel::updateSearchQuery,
            searchExpanded = uiState.searchExpanded,
            onSearchExpandedChange = logViewModel::setSearchExpanded,
        )
    }
}

@Composable
private fun LogTopBarActions(
    selectedLevel: LogDisplayLevel,
    onLevelSelected: (LogDisplayLevel) -> Unit,
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
            val context = LocalContext.current
            val levelItems = listOf(
                stringResource(R.string.log_all) to LogDisplayLevel.All,
                stringResource(R.string.log_debug) to LogDisplayLevel.Debug,
                stringResource(R.string.log_info) to LogDisplayLevel.Info,
                stringResource(R.string.log_warning) to LogDisplayLevel.Warning,
                stringResource(R.string.log_error) to LogDisplayLevel.Error,
            )
            val actionItems = listOf(
                stringResource(R.string.scroll_to_top) to onScrollTop,
                stringResource(R.string.scroll_to_bottom) to onScrollBottom,
                stringResource(R.string.export_log) to { logViewModel.exportLog(context) },
            )
            val optionSize = levelItems.size + actionItems.size
            ListPopupColumn {
                levelItems.forEachIndexed { index, item ->
                    key(index) {
                        DropdownImpl(
                            text = item.first,
                            optionSize = optionSize,
                            isSelected = selectedLevel == item.second,
                            onSelectedIndexChange = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                                onLevelSelected(item.second)
                                dismissState?.invoke()
                            },
                            index = index
                        )
                    }
                }
                actionItems.forEachIndexed { actionIndex, item ->
                    val index = levelItems.size + actionIndex
                    key(index) {
                        DropdownImpl(
                            text = item.first,
                            optionSize = optionSize,
                            isSelected = false,
                            onSelectedIndexChange = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                                item.second()
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
    isLoading: Boolean,
    isInitialLoadDone: Boolean,
    isTruncated: Boolean,
    emptyReason: LogEmptyReason?,
    emptyReasonDetail: String?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchExpanded: Boolean,
    onSearchExpandedChange: (Boolean) -> Unit,
) {
    val isWideScreen = LocalIsWideScreen.current
    val contentPadding = pageContentPadding(padding, padding, isWideScreen)
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading && !isInitialLoadDone) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                InfiniteProgressIndicator()
            }
        } else if (isInitialLoadDone && !hasAnyLog) {
            Text(
                text = logEmptyText(emptyReason, emptyReasonDetail),
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
                    if (isTruncated) {
                        item(key = "logTruncated") {
                            Text(
                                text = stringResource(R.string.logs_truncated),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                fontSize = 13.sp,
                                color = colorScheme.onSurfaceVariantSummary,
                            )
                        }
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

@Composable
private fun logEmptyText(reason: LogEmptyReason?, detail: String?): String {
    val fallback = stringResource(R.string.logs_empty)
    if (reason == null || detail.isNullOrBlank()) return fallback
    return when (reason) {
        LogEmptyReason.ReadFailed -> stringResource(R.string.logs_read_failed, detail)
        LogEmptyReason.FileLogFailed -> stringResource(R.string.logs_file_log_failed, detail)
    }
}

private fun String.matchesLogLevel(level: LogDisplayLevel): Boolean {
    return when (level) {
        LogDisplayLevel.All -> true
        LogDisplayLevel.Debug -> contains("调试") || contains("DEBUG")
        LogDisplayLevel.Info -> contains("信息") || contains("INFO")
        LogDisplayLevel.Warning -> contains("警告") || contains("WARN")
        LogDisplayLevel.Error -> contains("错误") || contains("ERROR")
    }
}
