package nep.timeline.cirno.handlers;

import java.util.Set;

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

    // ✅ 添加：全局音频播放状态（用于调试）
    private static volatile boolean globalAudioPlaying = false;

    public static void call(AppRecord appRecord, int event, int interfaceId) {
        // ✅ 添加：调试日志
        Log.d("AudioHandler.call() - 应用: " + appRecord.getPackageNameWithUser() + 
              ", 事件: " + eventToString(event) + ", interfaceId: " + interfaceId);
        
        Set<Integer> set = appRecord.getAppState().getInterfaceIds();
        
        // ✅ 修改：更激进的判断逻辑
        if (event == PLAYER_STATE_STARTED) {
            set.add(interfaceId);
            
            // ✅ 添加：直接解冻（不管集合状态）
            globalAudioPlaying = true;
            FreezerService.thaw(appRecord);
            Log.i("★ 音频播放开始: " + appRecord.getPackageNameWithUser() + 
                  " -> 立即解冻");
            
        } else if (event == PLAYER_STATE_RELEASED || event == PLAYER_STATE_IDLE || 
                   event == PLAYER_STATE_STOPPED || event == PLAYER_STATE_PAUSED) {
            set.remove(interfaceId);
            
            // ✅ 添加：集合变空时冻结
            if (set.isEmpty()) {
                globalAudioPlaying = false;
                if (appRecord.getAppState().setAudio(false)) {
                    Log.i("★ 音频播放停止: " + appRecord.getPackageNameWithUser() + 
                          " -> 6秒后冻结");
                    FreezerHandler.sendFreezeMessageIgnoreMessages(appRecord, 6000);
                }
            }
        }
    }
    
    // ✅ 添加：辅助方法（用于日志）
    private static String eventToString(int event) {
        switch(event) {
            case PLAYER_STATE_RELEASED: return "RELEASED(0)";
            case PLAYER_STATE_IDLE: return "IDLE(1)";
            case PLAYER_STATE_STARTED: return "STARTED(2)";
            case PLAYER_STATE_PAUSED: return "PAUSED(3)";
            case PLAYER_STATE_STOPPED: return "STOPPED(4)";
            default: return "UNKNOWN(" + event + ")";
        }
    }
    
    // ✅ 添加：获取全局音频状态（调试用）
    public static boolean isGlobalAudioPlaying() {
        return globalAudioPlaying;
    }
}
