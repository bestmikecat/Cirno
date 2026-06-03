package nep.timeline.cirno.ui.utils

import com.topjohnwu.superuser.io.SuFile

object RootFreezerRepository {
    fun isFrozenFreezerAvailable(): Boolean {
        return try {
            SuFile("/sys/fs/cgroup/frozen/cgroup.freeze").exists() &&
                SuFile("/sys/fs/cgroup/unfrozen/cgroup.freeze").exists()
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
