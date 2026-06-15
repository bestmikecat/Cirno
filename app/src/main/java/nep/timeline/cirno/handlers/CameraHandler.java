package nep.timeline.cirno.handlers;

import java.util.Set;

import nep.timeline.cirno.entity.AppRecord;
import nep.timeline.cirno.log.Log;
import nep.timeline.cirno.services.AppService;
import nep.timeline.cirno.services.FreezerService;
import nep.timeline.cirno.threads.FreezerHandler;

public class CameraHandler {
    public static final int CAMERA_STATE_ACTIVE = 1;
    public static final int CAMERA_STATE_IDLE = 2;
    public static final int CAMERA_STATE_CLOSED = 3;

    public static void call(String packageName, Set<Integer> userIds, String cameraId, int state) {
        if (packageName == null || packageName.isEmpty() || cameraId == null) {
            return;
        }

        if (userIds == null || userIds.isEmpty()) {
            return;
        }

        for (Integer userId : userIds) {
            if (userId == null) {
                continue;
            }

            AppRecord appRecord = AppService.get(packageName, userId);
            if (appRecord == null) {
                continue;
            }

            call(appRecord, cameraId, state);
        }
    }

    public static void call(AppRecord appRecord, String cameraId, int state) {
        if (appRecord == null || cameraId == null) {
            return;
        }

        if (state == CAMERA_STATE_ACTIVE) {
            if (appRecord.getAppState().addCameraId(cameraId)) {
                Log.d("应用 " + appRecord.getPackageNameWithUser() + " 开始使用摄像头");
                FreezerService.thaw(appRecord);
            }
            return;
        }

        if ((state == CAMERA_STATE_IDLE || state == CAMERA_STATE_CLOSED)
                && appRecord.getAppState().removeCameraId(cameraId)) {
            Log.d("应用 " + appRecord.getPackageNameWithUser() + " 停止使用摄像头");
            FreezerHandler.sendFreezeMessageIgnoreMessages(appRecord);
        }
    }

    public static void clearAll() {
        for (AppRecord appRecord : AppService.getAllRecordsSnapshot()) {
            if (appRecord == null) {
                continue;
            }

            if (appRecord.getAppState().clearCameraIds()) {
                FreezerHandler.sendFreezeMessageIgnoreMessages(appRecord);
            }
        }
    }
}
