package nep.timeline.cirno.services;

import android.os.Process;

import nep.timeline.cirno.entity.AppRecord;
import nep.timeline.cirno.log.Log;
import nep.timeline.cirno.reflect.CakeReflection;
import nep.timeline.cirno.threads.FreezerHandler;
import nep.timeline.cirno.virtuals.ProcessRecord;

public class BootFreezeService {
    private static final long BOOT_FREEZE_DELAY_MS = 10_000L;

    public static void freezeAll() {
        FreezerHandler.handler.postDelayed(BootFreezeService::doFreezeAll, BOOT_FREEZE_DELAY_MS);
    }

    private static void doFreezeAll() {
        int frozenCount = 0;
        try {
            Object pidsSelfLocked = ActivityManagerService.getPidsSelfLocked();
            if (pidsSelfLocked == null) {
                Log.w("开机冻结: mPidsSelfLocked 为空");
                return;
            }

            int size;
            synchronized (pidsSelfLocked) {
                size = (int) CakeReflection.callMethod(pidsSelfLocked, "size");
            }

            for (int i = 0; i < size; i++) {
                Object rawRecord;
                synchronized (pidsSelfLocked) {
                    rawRecord = CakeReflection.callMethod(pidsSelfLocked, "valueAt", i);
                }

                if (rawRecord == null)
                    continue;

                ProcessRecord processRecord = new ProcessRecord(rawRecord);
                if (processRecord.getRunningUid() <= Process.SYSTEM_UID)
                    continue;

                if (processRecord.isDeathProcess())
                    continue;

                AppRecord appRecord = processRecord.getAppRecord();
                if (appRecord == null || appRecord.isFrozen())
                    continue;

                FreezerService.freezer(appRecord);
                if (appRecord.isFrozen())
                    frozenCount++;
            }
        } catch (Throwable e) {
            Log.e("开机冻结失败", e);
            return;
        }
        Log.i("开机冻结完成，共冻结 " + frozenCount + " 个应用");
    }
}
