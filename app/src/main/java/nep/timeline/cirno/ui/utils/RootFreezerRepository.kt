package nep.timeline.cirno.ui.utils

import com.topjohnwu.superuser.io.SuFile

object RootFreezerRepository {
    fun isFrozenFreezerAvailable(): Boolean {
        return try {
            val frozenDir = SuFile("/sys/fs/cgroup/frozen")
            val unfrozenDir = SuFile("/sys/fs/cgroup/unfrozen")
            if (!frozenDir.exists() && !frozenDir.mkdir()) return false
            if (!unfrozenDir.exists() && !unfrozenDir.mkdir()) return false
            val frozenFreeze = SuFile("/sys/fs/cgroup/frozen/cgroup.freeze")
            val unfrozenFreeze = SuFile("/sys/fs/cgroup/unfrozen/cgroup.freeze")
            if (!frozenFreeze.exists() || !unfrozenFreeze.exists()) return false
            writeSuFile(frozenFreeze, "1") && writeSuFile(unfrozenFreeze, "0")
        } catch (_: Throwable) {
            false
        }
    }

    private fun writeSuFile(file: SuFile, content: String): Boolean {
        return try {
            file.writeText(content)
            true
        } catch (_: Throwable) {
            false
        }
    }

    fun isUidFreezerAvailable(): Boolean {
        return try {
            if (SuFile("/sys/fs/cgroup/uid_1000/cgroup.freeze").exists()) {
                SuFile("/sys/fs/cgroup/uid_0/cgroup.freeze").exists()
            } else {
                SuFile("/sys/fs/cgroup/system/uid_0/cgroup.freeze").exists()
            }
        } catch (_: Throwable) {
            false
        }
    }

    fun isAnyFreezerAvailable(): Boolean = isUidFreezerAvailable() || isFrozenFreezerAvailable()
}
