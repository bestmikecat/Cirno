package nep.timeline.cirno.handlers;

import java.util.Set;

import nep.timeline.cirno.configs.checkers.AppConfigs;
import nep.timeline.cirno.entity.AppRecord;
import nep.timeline.cirno.log.Log;
import nep.timeline.cirno.services.FreezerService;
import nep.timeline.cirno.threads.FreezerHandler;

public class AudioHandler {
    public static final int PLAYER_STATE_RELEASED = 0;
    public static final int PLAYER_STATE_IDLE = 1;
    public static final int PLAYER_STATE_STARTED = 2;
    public static final int PLAYER_STATE_PAUSED = 3;
    public static final int PLAYER_STATE_STOPPED = 4;
    public static final Set<Integer> LISTEN_EVENT = Set.of(
            PLAYER_STATE_RELEASED, PLAYER_STATE_IDLE, PLAYER_STATE_STARTED,
            PLAYER_STATE_PAUSED, PLAYER_STATE_STOPPED
    );

    public static void call(AppRecord appRecord, int event, int interfaceId) {
        boolean backgroundPlayAllowed = AppConfigs.isBackgroundPlayAllowed(
                appRecord.getPackageName(),
                appRecord.getUserId()
        );

        if (event == PLAYER_STATE_STARTED) {
            if (appRecord.getAppState().addAudioInterface(interfaceId) && backgroundPlayAllowed) {
                FreezerService.thaw(appRecord);
                Log.d("🎵 音频播放: " + appRecord.getPackageNameWithUser());
            }
        } else if (event == PLAYER_STATE_PAUSED || event == PLAYER_STATE_STOPPED ||
                event == PLAYER_STATE_RELEASED || event == PLAYER_STATE_IDLE) {

            if (appRecord.getAppState().removeAudioInterface(interfaceId) && backgroundPlayAllowed) {
                Log.d("🔇 音频停止: " + appRecord.getPackageNameWithUser());
                FreezerHandler.sendFreezeMessageIgnoreMessages(appRecord);
            }
        }
    }
}
