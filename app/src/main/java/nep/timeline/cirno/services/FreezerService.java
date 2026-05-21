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
            for (ProcessRecord processRecord : appRecord.getProcessRecords()) {
                if (processRecord.isDeathProcess() || processRecord.isFrozen()) {
                    continue;
                }
                if (AppConfigs.isProcessExcludedFromFreeze(appRecord.getPackageName(), appRecord.getUserId(), processRecord.getProcessName())) {
                    continue;
                }
                FrozenRW.frozen(processRecord.getRunningUid(), processRecord.getPid());
                processRecord.setFrozen(true);
            }
            appRecord.setFrozen(true);
            return;
        }

        for (ProcessRecord processRecord : appRecord.getProcessRecords()) {
            if (processRecord.isDeathProcess() || processRecord.isFrozen())
                continue;

            if (AppConfigs.isProcessExcludedFromFreeze(appRecord.getPackageName(), appRecord.getUserId(), processRecord.getProcessName())) {
                continue;
            }

            FrozenRW.frozen(processRecord.getRunningUid(), processRecord.getPid());
            processRecord.setFrozen(true);
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

            FrozenRW.thaw(processRecord.getRunningUid(), processRecord.getPid());
            processRecord.setFrozen(false);
        }

        appRecord.setFrozen(false);
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
        Thread waitingNotification = new Thread(new Runnable() {
            @Override
            public void run() {
                long startTime = System.currentTimeMillis();
                while(!Thread.currentThread().isInterrupted()) {
                    if (System.currentTimeMillis() - startTime > interval) {
                        appRecord.setWaitingNotification(false);
                        Log.d(appRecord.getPackageName() + " 等待消息通知超时");
                    }
                    try {
                        if(!appRecord.isWaitingNotification()) {
                            break;
                        }
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                FreezerHandler.sendFreezeMessageIgnoreMessages(appRecord);
            }
        });
        waitingNotification.start();
    }
}
