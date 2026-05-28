package nep.timeline.cirno.ui.page.material

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import nep.timeline.cirno.MainActivity.LogViewModelSingleton.logViewModel
import nep.timeline.cirno.R
import nep.timeline.cirno.ui.app.LocalNavigator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialLogPage(
    padding: PaddingValues,
) {
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
            if (!uiState.hasMore || uiState.isLoadingMore || totalItems <= 0) return@collect
            if (lastVisible >= totalItems - 5) {
                logViewModel.loadNextPage()
            }
        }
    }

    LaunchedEffect(uiState.pendingScrollToBottom, uiState.loadedLines.size, uiState.hasMore, uiState.isLoadingMore, uiState.searchQuery) {
        if (!uiState.pendingScrollToBottom) return@LaunchedEffect

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

    MaterialPageScaffold(
        title = stringResource(R.string.logs_title),
        padding = padding,
        navigationIcon = {
            IconButton(onClick = { navigator.pop() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        actions = {
            IconButton(onClick = { logViewModel.setSearchExpanded(!uiState.searchExpanded) }) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            MaterialLogMenu(
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
                },
            )
        },
    ) {
        item(key = "logSearch") {
            AnimatedVisibility(visible = uiState.searchExpanded) {
                MaterialLogSearchField(
                    query = uiState.searchQuery,
                    onQueryChange = logViewModel::updateSearchQuery,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                )
            }
        }

        if (uiState.isInitialLoadDone && uiState.loadedLines.isEmpty()) {
            item(key = "empty") {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 96.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.logs_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            items(filteredLines) { line ->
                MaterialLogLine(line = line)
            }
        }
    }
}

@Composable
private fun MaterialLogMenu(
    onScrollTop: () -> Unit,
    onScrollBottom: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.scroll_to_top)) },
                onClick = {
                    expanded = false
                    onScrollTop()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.scroll_to_bottom)) },
                onClick = {
                    expanded = false
                    onScrollBottom()
                },
            )
        }
    }
}

@Composable
private fun MaterialLogSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge,
        label = { Text(stringResource(R.string.search)) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
            )
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        ),
        shape = RoundedCornerShape(16.dp),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
            onSearch = { keyboardController?.hide() },
        ),
    )
}

@Composable
private fun MaterialLogLine(line: String) {
    SelectionContainer {
        Text(
            text = line,
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            color = when {
                line.contains("错误") || line.contains("ERROR") -> MaterialTheme.colorScheme.error
                line.contains("警告") || line.contains("WARN") -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurface
            },
        )
    }
}
