package nep.timeline.cirno.ui.utils

import nep.timeline.cirno.log.Log
import com.topjohnwu.superuser.io.SuFile

object RootFreezerRepository {
    private const val TAG = "RootFreezerRepository"

    fun isFrozenFreezerAvailable(): Boolean {
        return try {
            val frozenAvailable = SuFile("/sys/fs/cgroup/frozen/cgroup.freeze").exists()
            val unfrozenAvailable = SuFile("/sys/fs/cgroup/unfrozen/cgroup.freeze").exists()
            Log.d("$TAG: Frozen mode availability: frozen=$frozenAvailable, unfrozen=$unfrozenAvailable")
            frozenAvailable && unfrozenAvailable
        } catch (e: Throwable) {
            Log.e("$TAG: Failed to check Frozen mode availability", e)
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
