package nep.timeline.cirno.utils;

import android.os.Process;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import nep.timeline.cirno.GlobalVars;
import nep.timeline.cirno.configs.settings.GlobalSettings;
import nep.timeline.cirno.log.Log;

public class FrozenRW {
    public static final String cgroupV2 = "/sys/fs/cgroup";
    private static final String cgroupV2FrozenDir = cgroupV2 + "/frozen";
    private static final String cgroupV2UnfrozenDir = cgroupV2 + "/unfrozen";
    private static final String cgroupV2FrozenFreeze = cgroupV2FrozenDir + "/cgroup.freeze";
    private static final String cgroupV2UnfrozenFreeze = cgroupV2UnfrozenDir + "/cgroup.freeze";
    private static final String cgroupV2FrozenProcs = cgroupV2FrozenDir + "/cgroup.procs";
    private static final String cgroupV2UnfrozenProcs = cgroupV2UnfrozenDir + "/cgroup.procs";
    private static final String cgroupV2UidStandardCheck = cgroupV2 + "/uid_0/cgroup.freeze";
    private static final String cgroupV2UidIsolatedCheck = cgroupV2 + "/system/uid_0/cgroup.freeze";
    private static final boolean cgroupV2SysAppIsolated;

    static {
        String path = "/sys/fs/cgroup/uid_1000/cgroup.freeze";
        cgroupV2SysAppIsolated = !Files.exists(Paths.get(path));
    }

    public static boolean isUidFreezerAvailable() {
        if (cgroupV2SysAppIsolated) {
            return Files.exists(Paths.get(cgroupV2UidIsolatedCheck));
        }
        return Files.exists(Paths.get(cgroupV2UidStandardCheck));
    }

    public static boolean isFrozenFreezerAvailable() {
        File frozenDir = new File(cgroupV2FrozenDir);
        File unfrozenDir = new File(cgroupV2UnfrozenDir);
        if (!frozenDir.exists() && !frozenDir.mkdir()) {
            Log.w("Frozen 模式初始化失败: 无法创建 " + cgroupV2FrozenDir);
            return false;
        }
        if (!unfrozenDir.exists() && !unfrozenDir.mkdir()) {
            Log.w("Frozen 模式初始化失败: 无法创建 " + cgroupV2UnfrozenDir);
            return false;
        }

        if (!hasFrozenFreezerFiles()) {
            Log.w("Frozen 模式不可用: 缺少 frozen/unfrozen cgroup.freeze");
            return false;
        }

        boolean frozenReady = writeRaw(cgroupV2FrozenFreeze, "1");
        boolean unfrozenReady = writeRaw(cgroupV2UnfrozenFreeze, "0");
        return frozenReady && unfrozenReady;
    }

    public static String selectAvailableFreezerMode() {
        if (isUidFreezerAvailable()) {
            return GlobalSettings.FREEZER_MODE_UID;
        }
        if (isFrozenFreezerAvailable()) {
            return GlobalSettings.FREEZER_MODE_FROZEN;
        }
        return null;
    }

    private static boolean hasFrozenFreezerFiles() {
        return Files.exists(Paths.get(cgroupV2FrozenFreeze)) && Files.exists(Paths.get(cgroupV2UnfrozenFreeze));
    }

    private static boolean writeRaw(String path, String value) {
        try (FileOutputStream outputStream = new FileOutputStream(path)) {
            outputStream.write(value.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
            return true;
        } catch (IOException e) {
            Log.w("Frozen 模式写入失败: " + path, e);
            return false;
        }
    }

    private static boolean useFrozenMode() {
        GlobalSettings settings = GlobalVars.globalSettings;
        return settings != null && GlobalSettings.FREEZER_MODE_FROZEN.equals(settings.freezerMode);
    }

    private static boolean writeFrozen(int uid, int pid, int frozenState) {
        if (useFrozenMode()) {
            if (!isFrozenFreezerAvailable()) {
                Log.w("Frozen 模式不可用，无法写入 pid=" + pid);
                return false;
            }
            String path = frozenState == 1 ? cgroupV2FrozenProcs : cgroupV2UnfrozenProcs;
            if (!RWUtils.writeFrozen(path, pid)) {
                Log.w("Frozen 模式写入 cgroup.procs 失败: " + path + ", pid=" + pid);
                return false;
            }
            return true;
        }

        if (!cgroupV2SysAppIsolated) {
            return RWUtils.writeFrozen(cgroupV2 + "/uid_" + uid + "/pid_" + pid + "/cgroup.freeze", frozenState);
        }

        if (uid < Process.FIRST_APPLICATION_UID)
            return RWUtils.writeFrozen(cgroupV2 + "/system/uid_" + uid + "/pid_" + pid + "/cgroup.freeze", frozenState);
        else
            return RWUtils.writeFrozen(cgroupV2 + "/apps/uid_" + uid + "/pid_" + pid + "/cgroup.freeze", frozenState);
    }

    public static boolean frozen(int uid, int pid) {
        return writeFrozen(uid, pid, 1);
    }

    public static boolean thaw(int uid, int pid) {
        return writeFrozen(uid, pid, 0);
    }
}
