package nep.timeline.cirno

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nep.timeline.cirno.ui.utils.AppContext
import nep.timeline.cirno.ui.utils.BackgroundManager
import nep.timeline.cirno.ui.utils.ConfigBinderRepository
import nep.timeline.cirno.ui.viewModel.AppListViewModel
import nep.timeline.cirno.ui.viewModel.AppUiStateViewModel
import nep.timeline.cirno.ui.viewModel.LogViewModel
import nep.timeline.cirno.ui.viewModel.MonitorViewModel
import nep.timeline.cirno.ui.app.App
import nep.timeline.cirno.binder.BinderService

class MainActivity : ComponentActivity() {
    object AppListViewModelSingleton {
        val appListViewModel: AppListViewModel by lazy { AppListViewModel() }
    }

    object MonitorViewModelSingleton {
        val monitorViewModel: MonitorViewModel by lazy { MonitorViewModel() }
    }

    object LogViewModelSingleton {
        val logViewModel: LogViewModel by lazy { LogViewModel() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppContext.init(this)
        BackgroundManager.init(this)
        enableEdgeToEdge()
        val appUiStateViewModel = ViewModelProvider(this)[AppUiStateViewModel::class.java]
        setContent {
            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) {
                    BinderService.register(this@MainActivity)
                    ConfigBinderRepository.loadIntoMemory()
                }
                appUiStateViewModel.loadFromGlobalSettings()
            }
            App(active = true, appUiStateViewModel = appUiStateViewModel)
        }
    }
}
