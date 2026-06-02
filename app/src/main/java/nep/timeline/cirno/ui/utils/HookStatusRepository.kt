package nep.timeline.cirno.ui.utils

import nep.timeline.cirno.binder.BinderService
import nep.timeline.cirno.provide.StatusBinder

object HookStatusRepository {
    data class HookStatusSnapshot(
        val statusBinderAvailable: Boolean,
        val hasError: Boolean = false,
        val androidReady: Boolean = false,
        val systemUiReady: Boolean = false,
        val hookVersion: String? = null,
    )

    fun loadHookStatusSnapshot(): HookStatusSnapshot {
        BinderService.register(AppContext.context)
        val status = StatusBinder.getInstance() ?: return HookStatusSnapshot(statusBinderAvailable = false)
        return try {
            val version = status.hookVersion
            HookStatusSnapshot(
                statusBinderAvailable = true,
                hasError = status.getSignal("error") == "1",
                androidReady = status.getSignal("android_hook_ready") == "1",
                systemUiReady = status.getSignal("systemui_hook_ready") == "1",
                hookVersion = if (version.isNullOrBlank()) null else version,
            )
        } catch (_: Throwable) {
            HookStatusSnapshot(statusBinderAvailable = false)
        }
    }

    fun isReKernelAvailable(): Boolean {
        BinderService.register(AppContext.context)
        val status = StatusBinder.getInstance() ?: return false
        return try {
            status.isReKernelAvailable
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
