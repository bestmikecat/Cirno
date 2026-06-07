package nep.timeline.cirno.ui.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object RootConfigSaveScope {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun saveGlobalSettingsAsync(
        defaultError: String,
        onFailed: () -> Unit = {},
    ) {
        scope.launch {
            val error = if (RootConfigRepository.saveGlobalSettingsFromMemory()) {
                null
            } else {
                RootConfigRepository.getLastErrorOrDefault(defaultError)
            }
            if (error != null) {
                withContext(Dispatchers.Main) {
                    onFailed()
                    WindowUtils.showToast(error)
                }
            }
        }
    }

    fun saveGlobalSettingsAndThen(
        defaultError: String,
        onSuccess: () -> Unit,
        onFailed: () -> Unit = {},
    ) {
        scope.launch {
            val success = RootConfigRepository.saveGlobalSettingsFromMemory()
            withContext(Dispatchers.Main) {
                if (success) {
                    onSuccess()
                } else {
                    val error = RootConfigRepository.getLastErrorOrDefault(defaultError)
                    onFailed()
                    WindowUtils.showToast(error)
                }
            }
        }
    }
}
