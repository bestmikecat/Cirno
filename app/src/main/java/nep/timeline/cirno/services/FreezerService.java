package nep.timeline.cirno.services;

import java.util.List;

import nep.timeline.cirno.configs.checkers.AppConfigs;
import nep.timeline.cirno.configs.policy.FreezeExemption;
import nep.timeline.cirno.entity.AppRecord;
import nep.timeline.cirno.log.Log;
import nep.timeline.cirno.threads.FreezerHandler;
import nep.timeline.cirno.threads.Handlers;
import nep.timeline.cirno.utils.ForceAppStandbyListener;
import nep.timeline.cirno.utils.FreezeExemptionChecker;
import nep.timeline.cirno.utils.FrozenRW;
import nep.timeline.cirno.virtuals.ProcessRecord;

public class FreezerService {
    public static synchronized void freezer(AppRecord appRecord) {
        FreezeExemption exemption = FreezeExemptionChecker.check(appRecord);
        if (exemption != null) {
            return;
        }

        if (AppConfigs.isBlackApp(appRecord.getPackageName(), appRecord.getUserId())) {
            boolean hasFrozenProcess = false;
            for (ProcessRecord processRecord : appRecord.getProcessRecords()) {
                if (processRecord.isDeathProcess()) {
                    continue;
                }
                if (processRecord.isFrozen()) {
                    hasFrozenProcess = true;
                    continue;
                }
                if (AppConfigs.isProcessExcludedFromFreeze(appRecord.getPackageName(), appRecord.getUserId(), processRecord.getProcessName())) {
                    continue;
                }
                if (FrozenRW.frozen(processRecord.getRunningUid(), processRecord.getPid())) {
                    processRecord.setFrozen(true);
                    hasFrozenProcess = true;
                }
            }
            appRecord.setFrozen(hasFrozenProcess);
            return;
        }

        boolean hasFrozenProcess = false;
        for (ProcessRecord processRecord : appRecord.getProcessRecords()) {
            if (processRecord.isDeathProcess())
                continue;

            if (processRecord.isFrozen()) {
                hasFrozenProcess = true;
                continue;
            }

            if (AppConfigs.isProcessExcludedFromFreeze(appRecord.getPackageName(), appRecord.getUserId(), processRecord.getProcessName())) {
                continue;
            }

            if (FrozenRW.frozen(processRecord.getRunningUid(), processRecord.getPid())) {
                processRecord.setFrozen(true);
                hasFrozenProcess = true;
            }
        }

        if (!hasFrozenProcess) {
            appRecord.setFrozen(false);
            return;
        }

        Handlers.alarms.post(() -> {
            try {
                ForceAppStandbyListener.removeAlarmsForUid(appRecord);
            } catch (Exception e) {
                Log.e("移除警报失败", e);
            }
        });

        Handlers.network.post(() -> NetworkManagementService.socketDestroy(appRecord));

        appRecord.setFrozen(true);
    }

    public static synchronized void thaw(AppRecord appRecord) {
        FreezerHandler.removeAppMessage(appRecord);

        if (!appRecord.isFrozen())
            return;

        for (ProcessRecord processRecord : appRecord.getProcessRecords()) {
            if (processRecord.isDeathProcess() || !processRecord.isFrozen())
                continue;

            if (FrozenRW.thaw(processRecord.getRunningUid(), processRecord.getPid())) {
                processRecord.setFrozen(false);
            }
        }

        appRecord.setFrozen(hasFrozenProcess(appRecord));
    }

    public static void temporaryUnfreezeIfNeed(int uid, String reason, long interval) {
        List<AppRecord> appRecords = AppService.getByUid(uid);
        if (appRecords.isEmpty())
            return;

        for (AppRecord appRecord : appRecords) {
            if (appRecord == null)
                continue;

            temporaryUnfreezeIfNeed(appRecord, reason, interval);
        }
    }

    public static void temporaryUnfreezeIfNeed(String packageName, int userId, String reason, long interval) {
        temporaryUnfreezeIfNeed(AppService.get(packageName, userId), reason, interval);
    }

    public static void temporaryUnfreezeIfNeed(AppRecord appRecord, String reason, long interval) {
        if (appRecord == null)
            return;

        boolean blacklisted = AppConfigs.isBlackApp(appRecord.getPackageName(), appRecord.getUserId());
        if (!blacklisted && appRecord.isSystem())
            return;

        if (appRecord.isFrozen())
            Log.i(appRecord.getPackageNameWithUser() + " " + reason);

        thaw(appRecord);
        if (appRecord.isWaitingNotification()) {
            FreezerHandler.sendWaitingNotificationFreezeMessage(appRecord, interval);
        } else {
            FreezerHandler.sendTemporaryFreezeMessage(appRecord, interval);
        }
    }

    private static boolean hasFrozenProcess(AppRecord appRecord) {
        for (ProcessRecord processRecord : appRecord.getProcessRecords()) {
            if (processRecord != null && !processRecord.isDeathProcess() && processRecord.isFrozen()) {
                return true;
            }
        }
        return false;
    }
}
