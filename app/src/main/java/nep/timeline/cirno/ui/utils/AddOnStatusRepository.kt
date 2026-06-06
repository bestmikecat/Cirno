package nep.timeline.cirno.ui.utils

import com.topjohnwu.superuser.io.SuFile

object AddOnStatusRepository {
    private const val TOMB_STONE_MODULE_PROP = "/data/adb/modules/lib_tombstone/module.prop"
    private const val TOMB_STONE_MODULE_DISABLE = "/data/adb/modules/lib_tombstone/disable"

    fun isAddOnEnabled(): Boolean {
        return try {
            val moduleProp = SuFile(TOMB_STONE_MODULE_PROP)
            val disableFile = SuFile(TOMB_STONE_MODULE_DISABLE)
            
            // 模块存在 且 disable文件不存在
            moduleProp.exists() && !disableFile.exists()
        } catch (_: Throwable) {
            false
        }
    }
}
