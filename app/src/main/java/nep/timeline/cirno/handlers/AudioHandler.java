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

    public static void call(AppRecord appRecord, int event, int interfaceId) {
        Log.d("AudioHandler.call() - 应用: " + appRecord.getPackageNameWithUser() + 
              ", 事件: " + eventToString(event) + ", interfaceId: " + interfaceId);
        
        Set<Integer> set = appRecord.getAppState().getInterfaceIds();
        
        // ✅ 修改：STARTED ���添加到集合
        if (event == PLAYER_STATE_STARTED) {
            boolean wasEmpty = set.isEmpty();
            set.add(interfaceId);
            
            Log.d("  集合状态: " + set.size() + " 个播放器");
            
            // 从无音频到有音频，解冻
            if (wasEmpty) {
                appRecord.getAppState().setAudio(true);
                FreezerService.thaw(appRecord);
                Log.i("🎵 音频开始，解冻应用: " + appRecord.getPackageNameWithUser());
            }
            
        } else if (event == PLAYER_STATE_PAUSED || event == PLAYER_STATE_STOPPED || 
                   event == PLAYER_STATE_RELEASED || event == PLAYER_STATE_IDLE) {
            
            // ✅ 修改：所有停止事件都从集合中移除
            boolean removed = set.remove(interfaceId);
            
            Log.d("  移除播放器" + interfaceId + (removed ? " (成功)" : " (不存在)") + 
                  ", 剩余: " + set.size() + " 个");
            
            // 从有音频到无音频，冻结
            if (set.isEmpty()) {
                appRecord.getAppState().setAudio(false);
                Log.i("🔇 音频停止，6秒后冻结应用: " + appRecord.getPackageNameWithUser());
                FreezerHandler.sendFreezeMessageIgnoreMessages(appRecord, 6000);
            }
        }
    }
    
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
}