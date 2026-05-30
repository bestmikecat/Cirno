package nep.timeline.cirno.handlers;

import nep.timeline.cirno.configs.checkers.AppConfigs;
import nep.timeline.cirno.entity.AppRecord;
import nep.timeline.cirno.log.Log;
import nep.timeline.cirno.services.FreezerService;
import nep.timeline.cirno.threads.FreezerHandler;

public class RecordingHandler {
    public static final int RECORD_RIID_INVALID = -1;
    public static final int RECORDER_STATE_STARTED = 0;
    public static final int RECORDER_STATE_STOPPED = 1;

    public static void call(AppRecord appRecord, int event, int riid) {
        if (event == RECORDER_STATE_STARTED) {
            if (!appRecord.getAppState().addRecordingId(riid))
                return;
            Log.d("应用 " + appRecord.getPackageNameWithUser() + " 开始录音");
            if (AppConfigs.isRecordingAllowed(appRecord.getPackageName(), appRecord.getUserId())) {
                FreezerService.thaw(appRecord);
            }
            return;
        }

        if (appRecord.getAppState().removeRecordingId(riid)) {
            Log.d("应用 " + appRecord.getPackageNameWithUser() + " 停止录音");
            FreezerHandler.sendFreezeMessageIgnoreMessages(appRecord);
        }
    }
}
