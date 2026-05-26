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
import nep.timeline.cirno.GlobalVars
import nep.timeline.cirno.ui.app.AppTheme
import nep.timeline.cirno.ui.ApplicationHome
import nep.timeline.cirno.ui.utils.AppContext
import nep.timeline.cirno.ui.utils.ConfigBinderRepository

class ApplicationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppContext.init(this)
        enableEdgeToEdge()
        setContent {
            var configLoaded by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) {
                    ConfigBinderRepository.loadIntoMemory()
                }
                configLoaded = true
            }
            AppTheme(
                uiStyle = if (configLoaded) GlobalVars.globalSettings?.uiStyle ?: 0 else 0,
                colorMode = if (configLoaded) GlobalVars.globalSettings?.colorMode ?: 0 else 0,
            ) {
                ApplicationHome(this)
            }
        }
    }
}
