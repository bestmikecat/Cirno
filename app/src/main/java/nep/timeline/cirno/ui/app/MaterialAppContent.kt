package nep.timeline.cirno.ui.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.NavDisplayTransitionEffects
import nep.timeline.cirno.R
import nep.timeline.cirno.MainActivity.AppListViewModelSingleton.appListViewModel
import nep.timeline.cirno.ui.navigation3.Navigator
import nep.timeline.cirno.ui.navigation3.Route
import nep.timeline.cirno.ui.page.AboutPage
import nep.timeline.cirno.ui.page.AppPage
import nep.timeline.cirno.ui.page.LogPage
import nep.timeline.cirno.ui.page.material.MaterialInfoPage
import nep.timeline.cirno.ui.page.material.MaterialSettingsPage
import nep.timeline.cirno.ui.utils.AppContext
import nep.timeline.cirno.ui.utils.shouldShowSplitPane

private data class MaterialNavItem(
    val label: String,
    val icon: ImageVector,
)

@Composable
fun MaterialAppContent(
    active: Boolean,
    padding: PaddingValues,
) {
    val pageCount = if (active) 3 else 2
    val settingsPageIndex = if (active) 2 else 1
    val pagerState = rememberPagerState(pageCount = { pageCount })
    val mainPagerState = rememberMainPagerState(pagerState)
    LaunchedEffect(mainPagerState.pagerState.currentPage) {
        mainPagerState.syncPage()
    }

    val backStack = remember { mutableStateListOf<NavKey>().apply { add(Route.Main) } }
    val navigator = remember { Navigator(backStack) }
    val isWideScreen = shouldShowSplitPane()
    val navigationItems = remember(active) {
        if (active) {
            listOf(
                MaterialNavItem(AppContext.context.getString(R.string.main), Icons.Outlined.Home),
                MaterialNavItem(AppContext.context.getString(R.string.app_list), Icons.Outlined.Apps),
                MaterialNavItem(AppContext.context.getString(R.string.settings), Icons.Outlined.Settings),
            )
        } else {
            listOf(
                MaterialNavItem(AppContext.context.getString(R.string.main), Icons.Outlined.Home),
                MaterialNavItem(AppContext.context.getString(R.string.settings), Icons.Outlined.Settings),
            )
        }
    }

    MainScreenBackHandler(mainPagerState, navigator)

    CompositionLocalProvider(
        LocalNavigator provides navigator,
        LocalMainPagerState provides mainPagerState,
        LocalIsWideScreen provides isWideScreen,
    ) {
        val entryProvider = remember(backStack) {
            entryProvider<NavKey> {
                entry<Route.Main> {
                    MaterialHome(
                        padding = padding,
                        navigationItems = navigationItems,
                        mainPagerState = mainPagerState,
                        settingsPageIndex = settingsPageIndex,
                    )
                }
                entry<Route.About> {
                    AboutPage(padding = padding)
                }
                entry<Route.Log> {
                    LogPage(padding = padding)
                }
            }
        }

        val entries = rememberDecoratedNavEntries(
            backStack = backStack,
            entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator()),
            entryProvider = entryProvider,
        )

        val appState = LocalAppState.current
        val transitionEffects = remember(
            appState.enableCornerClip,
            appState.enableDim,
            appState.blockInputDuringTransition,
            appState.popDirectionFollowsSwipeEdge,
        ) {
            NavDisplayTransitionEffects(
                enableCornerClip = appState.enableCornerClip,
                dimAmount = if (appState.enableDim) 0.5f else 0f,
                blockInputDuringTransition = appState.blockInputDuringTransition,
                popDirectionFollowsSwipeEdge = appState.popDirectionFollowsSwipeEdge,
            )
        }

        Surface(color = MaterialTheme.colorScheme.background) {
            NavDisplay(
                entries = entries,
                onBack = { navigator.pop() },
                transitionEffects = transitionEffects,
            )
        }
    }
}

@Composable
private fun MaterialHome(
    padding: PaddingValues,
    navigationItems: List<MaterialNavItem>,
    mainPagerState: MainPagerState,
    settingsPageIndex: Int,
) {
    val isWideScreen = LocalIsWideScreen.current
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout),
        bottomBar = {
            if (!isWideScreen) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    navigationItems.forEachIndexed { index, item ->
                        NavigationBarItem(
                            selected = mainPagerState.selectedPage == index,
                            onClick = { mainPagerState.animateToPage(index) },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        if (isWideScreen) {
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    Column(
                        modifier = Modifier.padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        navigationItems.forEachIndexed { index, item ->
                            NavigationRailItem(
                                selected = mainPagerState.selectedPage == index,
                                onClick = { mainPagerState.animateToPage(index) },
                                icon = { Icon(item.icon, contentDescription = item.label) },
                                label = { Text(item.label) },
                            )
                        }
                    }
                }
                Box(modifier = Modifier.weight(1f)) {
                    MaterialAppPager(
                        active = settingsPageIndex == 2,
                        padding = PaddingValues(top = innerPadding.calculateTopPadding()),
                        pagerState = mainPagerState,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        } else {
            MaterialAppPager(
                active = settingsPageIndex == 2,
                padding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + innerPadding.calculateBottomPadding(),
                ),
                pagerState = mainPagerState,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun MaterialAppPager(
    active: Boolean,
    padding: PaddingValues,
    pagerState: MainPagerState,
    modifier: Modifier = Modifier,
) {
    val appState = LocalAppState.current
    HorizontalPager(
        state = pagerState.pagerState,
        modifier = modifier,
        userScrollEnabled = appState.enablePageUserScroll,
        verticalAlignment = Alignment.Top,
    ) { page ->
        when (page) {
            0 -> MaterialInfoPage(
                callback = { pagerState.animateToPage(it) },
                padding = padding,
            )
            1 -> if (active) {
                AppPage(
                    viewModel = appListViewModel,
                    padding = padding,
                    scrollEndHaptic = appState.enableScrollEndHaptic,
                )
            } else {
                MaterialSettingsPage(
                    active = false,
                    padding = padding,
                )
            }
            else -> MaterialSettingsPage(
                active = true,
                padding = padding,
            )
        }
    }
}
