package nep.timeline.cirno

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nep.timeline.cirno.ui.utils.AppContext
import nep.timeline.cirno.ui.utils.ConfigBinderRepository
import nep.timeline.cirno.ui.viewModel.AppListViewModel
import nep.timeline.cirno.ui.viewModel.LogViewModel
import nep.timeline.cirno.ui.app.App
import nep.timeline.cirno.binder.BinderService

class MainActivity : ComponentActivity() {
    object AppListViewModelSingleton {
        val appListViewModel: AppListViewModel by lazy { AppListViewModel() }
    }

    object LogViewModelSingleton {
        val logViewModel: LogViewModel by lazy { LogViewModel() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppContext.init(this)
        enableEdgeToEdge()
        setContent {
            var configLoaded by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) {
                    BinderService.register(this@MainActivity)
                    ConfigBinderRepository.loadIntoMemory()
                }
                configLoaded = true
            }
            App(active = true, configLoadKey = configLoaded)
        }
    }
}
