package nep.timeline.cirno.ui.utils

import nep.timeline.cirno.binder.BinderService
import nep.timeline.cirno.provide.StatusBinder

object HookStatusRepository {
    data class HookStatusSnapshot(
        val statusBinderAvailable: Boolean,
        val hasError: Boolean = false,
        val hookVersion: String? = null,
        val deviceType: String? = null,
        val addOnRequired: Boolean = false,
        val hookType: String? = null,
    )

    fun loadHookStatusSnapshot(): HookStatusSnapshot {
        BinderService.register(AppContext.context)
        val status = StatusBinder.getInstance() ?: return HookStatusSnapshot(statusBinderAvailable = false)
        return try {
            val version = status.hookVersion
            HookStatusSnapshot(
                statusBinderAvailable = true,
                hasError = status.getSignal("error") == "1",
                hookVersion = if (version.isNullOrBlank()) null else version,
                deviceType = status.getSignal("device_type").takeIf { !it.isNullOrBlank() },
                addOnRequired = status.getSignal("add_on_required") == "1",
                hookType = status.getSignal("hook_type").takeIf { !it.isNullOrBlank() },
            )
        } catch (_: Throwable) {
            HookStatusSnapshot(statusBinderAvailable = false)
        }
    }

    fun isPacketAvailable(): Boolean {
        BinderService.register(AppContext.context)
        val status = StatusBinder.getInstance() ?: return false
        return try {
            status.isPacketAvailable
        } catch (_: Throwable) {
            false
        }
    }

    fun getHookVersion(): String? {
        BinderService.register(AppContext.context)
        val status = StatusBinder.getInstance() ?: return null
        return try {
            status.hookVersion.takeIf { !it.isNullOrBlank() }
        } catch (_: Throwable) {
            null
        }
    }
}
