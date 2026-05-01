package nep.timeline.cirno.handlers;

import nep.timeline.cirno.configs.checkers.AppConfigs;
import nep.timeline.cirno.entity.AppRecord;
import nep.timeline.cirno.log.Log;
import nep.timeline.cirno.services.FreezerService;
import nep.timeline.cirno.threads.FreezerHandler;

public class LocationHandler {
    public static void call(AppRecord appRecord) {
        if (appRecord.isSystem())
            return;

        boolean locationUseAllowed = AppConfigs.isLocationUseAllowed(
                appRecord.getPackageName(),
                appRecord.getUserId()
        );

        if (appRecord.getAppState().isLocation()) {
            if (!locationUseAllowed)
                return;
            Log.d("应用 " + appRecord.getPackageNameWithUser() + " 开始定位");
            FreezerService.thaw(appRecord);
            return;
        }

        if (locationUseAllowed) {
            Log.d("应用 " + appRecord.getPackageNameWithUser() + " 结束定位");
            FreezerHandler.sendFreezeMessage(appRecord);
        }
    }
}
