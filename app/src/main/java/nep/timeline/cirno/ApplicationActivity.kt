package nep.timeline.cirno

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import nep.timeline.cirno.ui.ApplicationHome
import nep.timeline.cirno.ui.utils.AppContext
import nep.timeline.cirno.ui.utils.ConfigBinderRepository
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

class ApplicationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppContext.init(this)
        lifecycleScope.launch(Dispatchers.IO) {
            ConfigBinderRepository.loadIntoMemory()
        }
        enableEdgeToEdge()
        setContent {
            MiuixTheme(
                colors = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
            ) {
                ApplicationHome(this)
            }
        }
    }
}
