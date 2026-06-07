package nep.timeline.cirno.ui.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateListOf
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplayTransitionEffects
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import nep.timeline.cirno.ui.navigation3.Navigator
import nep.timeline.cirno.ui.navigation3.Route

@Stable
data class NavigationShellState(
    val backStack: androidx.compose.runtime.snapshots.SnapshotStateList<NavKey>,
    val navigator: Navigator,
)

@Composable
fun rememberNavigationShellState(): NavigationShellState {
    val backStack = remember { mutableStateListOf<NavKey>().apply { add(Route.Main) } }
    val navigator = remember(backStack) { Navigator(backStack) }
    return NavigationShellState(backStack = backStack, navigator = navigator)
}

@Composable
fun NavigationShellBackHandler(
    isBackEnabled: Boolean,
    onBackCompleted: () -> Unit,
) {
    val navEventState = rememberNavigationEventState(NavigationEventInfo.None)
    NavigationBackHandler(
        state = navEventState,
        isBackEnabled = isBackEnabled,
        onBackCompleted = onBackCompleted,
    )
}

@Composable
fun rememberMainPagerBackEnabled(selectedPage: Int, navigator: Navigator): Boolean {
    return remember(selectedPage, navigator) {
        derivedStateOf {
            navigator.current() is Route.Main && navigator.backStackSize() == 1 && selectedPage != 0
        }
    }.value
}

@Composable
fun rememberSharedTransitionEffects(appState: AppState): NavDisplayTransitionEffects {
    return remember(
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
}
