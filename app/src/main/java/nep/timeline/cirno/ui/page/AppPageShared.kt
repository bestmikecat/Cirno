package nep.timeline.cirno.ui.page

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nep.timeline.cirno.entity.AppItem
import nep.timeline.cirno.ui.viewModel.AppListViewModel

data class AppListScreenState(
    val filteredApps: List<AppItem>,
    val loadingApps: List<AppItem>,
    val searchValue: String,
    val type: Int,
    val updatedApps: Boolean,
)

@Composable
fun rememberAppListScreenState(viewModel: AppListViewModel): AppListScreenState {
    val filteredApps by viewModel.cacheFilterApps.collectAsStateWithLifecycle()
    val loadingApps by viewModel.filterApps.collectAsStateWithLifecycle()
    val searchValue by viewModel.search.collectAsStateWithLifecycle()
    val type by viewModel.type.collectAsStateWithLifecycle()
    val updatedApps by viewModel.updatedApps.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    val isActive = remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                isActive.value = true
            } else if (event == Lifecycle.Event.ON_STOP) {
                isActive.value = false
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    androidx.compose.runtime.LaunchedEffect(isActive.value, type) {
        viewModel.getFilterApps()
    }

    return AppListScreenState(
        filteredApps = filteredApps,
        loadingApps = loadingApps,
        searchValue = searchValue,
        type = type,
        updatedApps = updatedApps,
    )
}
