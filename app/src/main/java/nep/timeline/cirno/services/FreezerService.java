package nep.timeline.cirno.services;

import java.util.List;

import nep.timeline.cirno.entity.AppRecord;
import nep.timeline.cirno.threads.Handlers;
import nep.timeline.cirno.utils.ForceAppStandbyListener;
import nep.timeline.cirno.virtuals.ProcessRecord;
import nep.timeline.cirno.log.Log;
import nep.timeline.cirno.threads.FreezerHandler;
import nep.timeline.cirno.utils.FrozenRW;

public class FreezerService {
    public static void freezer(AppRecord appRecord) {
        // ✅ 修改：增加日志便于诊断
        Log.d("freezer() 检查条件: " + appRecord.getPackageNameWithUser() +
              ", frozen=" + appRecord.isFrozen() +
              ", isSystem=" + appRecord.isSystem() +
              ", visible=" + appRecord.getAppState().isVisible() +
              ", audio=" + appRecord.getAppState().isAudio() +
              ", location=" + appRecord.getAppState().isLocation() +
              ", recording=" + appRecord.getAppState().isRecording() +
              ", vpn=" + appRecord.getAppState().isVpn());
        
        if (appRecord.isFrozen() || appRecord.isSystem() || 
            appRecord.getAppState().isVisible() || 
            appRecord.getAppState().isLocation() || 
            appRecord.getAppState().isAudio() ||           // ← 如果正在播放音频，不冻结
            appRecord.getAppState().isRecording() || 
            appRecord.getAppState().isVpn()) {
            Log.d("freezer() 条件不满足，不冻结");
            return;
        }

        Log.i("freezer() 开始冻结: " + appRecord.getPackageNameWithUser());

        for (ProcessRecord processRecord : appRecord.getProcessRecords()) {
            if (processRecord.isDeathProcess() || processRecord.isFrozen())
                continue;

            FrozenRW.frozen(processRecord.getRunningUid(), processRecord.getPid());
            processRecord.setFrozen(true);
        }

        Handlers.alarms.post(() -> ForceAppStandbyListener.removeAlarmsForUid(appRecord));
        Handlers.network.post(() -> {
            try {
                NetworkManagementService.socketDestroy(appRecord);
            } catch (UnsupportedOperationException e) {
                Log.w("socketDestroy 不支持，跳过");
            } catch (Exception e) {
                Log.e("socketDestroy 失败", e);
            }
        });
        
        appRecord.setFrozen(true);
        Log.i("  ✓ 应用已冻结: " + appRecord.getPackageNameWithUser());
    }

    public static void thaw(AppRecord appRecord) {
        Log.i("thaw() 解冻: " + appRecord.getPackageNameWithUser());
        
        FreezerHandler.removeAppMessage(appRecord);

        if (!appRecord.isFrozen()) {
            Log.d("  应用未被冻结，跳过");
            return;
        }

        for (ProcessRecord processRecord : appRecord.getProcessRecords()) {
            if (processRecord.isDeathProcess() || !processRecord.isFrozen())
                continue;

            FrozenRW.thaw(processRecord.getRunningUid(), processRecord.getPid());
            processRecord.setFrozen(false);
        }

        appRecord.setFrozen(false);
        Log.i("  ✓ 应用已解冻: " + appRecord.getPackageNameWithUser());
    }

    public static void temporaryUnfreezeIfNeed(int uid, String reason, long interval) {
        List<AppRecord> appRecords = AppService.getByUid(uid);

        if (appRecords == null || appRecords.isEmpty())
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
        if (appRecord == null || appRecord.isSystem())
            return;

        if (appRecord.isFrozen())
            Log.i(appRecord.getPackageNameWithUser() + " " + reason);

        thaw(appRecord);
        FreezerHandler.sendFreezeMessageIgnoreMessages(appRecord, interval);
    }
}
