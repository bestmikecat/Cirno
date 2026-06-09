package nep.timeline.cirno.services;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import nep.timeline.cirno.reflect.CakeReflection;
import nep.timeline.cirno.entity.AppRecord;
import nep.timeline.cirno.log.Log;
import nep.timeline.cirno.threads.FreezerHandler;
import nep.timeline.cirno.utils.FrozenRW;
import nep.timeline.cirno.virtuals.ProcessRecord;

public class ProcessService {
    private static final Map<String, Map<Integer, ProcessRecord>> PROCESS_NAME_MAP = new ConcurrentHashMap<>();
    private static final Object lock = new Object();

    public static void addProcessRecord(Object record) {
        ProcessRecord processRecord = new ProcessRecord(record);
        AppRecord appRecord = processRecord.getAppRecord();
        if (appRecord == null)
            return;

        synchronized (lock) {
            PROCESS_NAME_MAP.computeIfAbsent(processRecord.getProcessName(), k -> new ConcurrentHashMap<>()).put(processRecord.getRunningUid(), processRecord);
            appRecord.getProcessRecords().add(processRecord);
        }

        FreezerHandler.sendFreezeMessage(appRecord);
    }

    public static AppRecord removeProcessRecord(ProcessRecord processRecord) {
        return removeProcessRecord(processRecord.getProcessName(), processRecord.getRunningUid(), true, null);
    }

    public static AppRecord removeProcessRecordWithoutThaw(ProcessRecord processRecord, String path) {
        return removeProcessRecord(processRecord.getProcessName(), processRecord.getRunningUid(), false, path);
    }

    public static AppRecord removeProcessRecord(String name, int uid) {
        return removeProcessRecord(name, uid, true, null);
    }

    private static AppRecord removeProcessRecord(String name, int uid, boolean thawOnRemove, String path) {
        ProcessRecord processRecord;
        AppRecord appRecord;
        boolean shouldThaw;
        int thawUid;
        int thawPid;
        String processName;
        synchronized (lock) {
            Map<Integer, ProcessRecord> records = PROCESS_NAME_MAP.get(name);
            if (records == null)
                return null;
            processRecord = records.remove(uid);
            if (processRecord == null)
                return null;
            if (records.isEmpty())
                PROCESS_NAME_MAP.remove(name, records);
            shouldThaw = processRecord.isFrozen();
            thawUid = processRecord.getRunningUid();
            thawPid = processRecord.getPid();
            processName = processRecord.getProcessName();
            appRecord = processRecord.getAppRecord();
            if (appRecord != null) {
                appRecord.getProcessRecords().remove(processRecord);
                if (appRecord.getProcessRecords().isEmpty())
                    appRecord.reset();
                else
                    appRecord.setFrozen(hasFrozenProcess(appRecord));
            }
        }
        if (thawOnRemove && shouldThaw)
            FrozenRW.thaw(thawUid, thawPid);
        else if (!thawOnRemove && thawPid > 0)
            FrozenRW.thawQuietly(thawUid, thawPid);
        if (!thawOnRemove) {
            String packageName = appRecord == null ? processRecord.getPackageName() : appRecord.getPackageNameWithUser();
            Log.d(packageName + " 进程 " + processName + "(pid=" + thawPid + ") 被取消管理，路径: " + path);
        }
        return appRecord;
    }

    private static boolean hasFrozenProcess(AppRecord appRecord) {
        for (ProcessRecord processRecord : appRecord.getProcessRecords()) {
            if (processRecord != null && !processRecord.isDeathProcess() && processRecord.isFrozen())
                return true;
        }
        return false;
    }

    public static ProcessRecord getProcessRecord(Object record) {
        if (record == null)
            return null;
        ProcessRecord processRecord = new ProcessRecord(record);
        return getProcessRecord(processRecord.getProcessName(), processRecord.getRunningUid());
    }

    public static ProcessRecord getProcessRecord(String processName, int uid) {
        if (processName == null || processName.isEmpty())
            return null;
        Map<Integer, ProcessRecord> map = PROCESS_NAME_MAP.get(processName);
        if (map == null)
            return null;
        return map.get(uid);
    }

    public static ProcessRecord getProcessRecordByPid(int pid) {
        ProcessRecord processRecord;
        Object mPidsSelfLocked = ActivityManagerService.getPidsSelfLocked();
        synchronized (mPidsSelfLocked) {
            processRecord = getProcessRecord(CakeReflection.callMethod(mPidsSelfLocked, "get", pid));
        }
        return processRecord;
    }
}
