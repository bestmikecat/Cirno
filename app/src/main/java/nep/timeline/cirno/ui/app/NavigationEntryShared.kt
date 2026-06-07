package nep.timeline.cirno.ui.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import nep.timeline.cirno.ui.navigation3.Route

@Composable
fun rememberSharedNavigationEntries(
    backStack: SnapshotStateList<NavKey>,
    mainContent: @Composable () -> Unit,
    aboutContent: @Composable () -> Unit,
    logContent: @Composable () -> Unit,
) = remember(backStack) {
    entryProvider<NavKey> {
        entry<Route.Main> { mainContent() }
        entry<Route.About> { aboutContent() }
        entry<Route.Log> { logContent() }
    }
}
