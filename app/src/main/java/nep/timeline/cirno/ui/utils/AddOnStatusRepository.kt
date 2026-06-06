package nep.timeline.cirno.ui.utils

import com.topjohnwu.superuser.io.SuFile

object AddOnStatusRepository {
    private const val TOMB_STONE_MODULE_PROP = "/data/adb/modules/lib_tombstone/module.prop"

    fun isAddOnEnabled(): Boolean {
        return try {
            SuFile(TOMB_STONE_MODULE_PROP).exists()
        } catch (_: Throwable) {
            false
        }
    }
}
