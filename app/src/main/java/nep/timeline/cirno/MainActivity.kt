package nep.timeline.cirno

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import nep.timeline.cirno.ui.utils.AppContext
import nep.timeline.cirno.ui.viewModel.AppListViewModel
import nep.timeline.cirno.ui.app.App
import nep.timeline.cirno.binder.BinderService
import nep.timeline.cirno.configs.ConfigManager
import nep.timeline.cirno.ui.dialog.RootDialog
import nep.timeline.cirno.utils.EnvUtils
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

class MainActivity : ComponentActivity() {
    object AppListViewModelSingleton {
        val appListViewModel: AppListViewModel by lazy { AppListViewModel() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppContext.init(this)
        if (!ConfigManager.manager.readConfigSU()) {
            ConfigManager.manager.readConfig()
        }
        BinderService.register(this)
        enableEdgeToEdge()
        setContent {
            val showRootDialog = rememberSaveable { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                if (!EnvUtils.checkRoot()) {
                    showRootDialog.value = true
                }
            }

            MiuixTheme(
                colors = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
            ) {
                App(active = true)
                RootDialog(showDialog = showRootDialog)
            }
        }
    }
}
