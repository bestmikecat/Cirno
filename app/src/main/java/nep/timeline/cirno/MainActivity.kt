package nep.timeline.cirno

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import nep.timeline.cirno.ui.utils.AppContext
import nep.timeline.cirno.ui.viewModel.AppListViewModel
import nep.timeline.cirno.ui.app.App
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
        enableEdgeToEdge()
        setContent {
            MiuixTheme(
                colors = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
            ) {
                App(active = true)
            }
        }
    }
}
