package nep.timeline.cirno

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nep.timeline.cirno.configs.ConfigManager
import nep.timeline.cirno.ui.dialog.RootDialog
import nep.timeline.cirno.ui.utils.AppContext
import nep.timeline.cirno.ui.utils.BackgroundManager
import nep.timeline.cirno.ui.viewModel.AppListViewModel
import nep.timeline.cirno.ui.viewModel.AppUiStateViewModel
import nep.timeline.cirno.ui.viewModel.LogViewModel
import nep.timeline.cirno.ui.viewModel.MonitorViewModel
import nep.timeline.cirno.ui.app.App
import nep.timeline.cirno.binder.BinderService
import nep.timeline.cirno.utils.EnvUtils

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
        if (!ConfigManager.manager.readConfigSU()) {
            ConfigManager.manager.saveConfigSU()
        }
        enableEdgeToEdge()
        val appUiStateViewModel = ViewModelProvider(this)[AppUiStateViewModel::class.java]
        setContent {
            val showRootDialog = rememberSaveable { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                val lacksRoot = withContext(Dispatchers.IO) {
                    BinderService.register(this@MainActivity)
                    !EnvUtils.checkRoot()
                }
                if (lacksRoot) {
                    showRootDialog.value = true
                }
                appUiStateViewModel.loadFromGlobalSettings()
            }
            App(active = true, appUiStateViewModel = appUiStateViewModel)
            RootDialog(showDialog = showRootDialog)
        }
    }
}
