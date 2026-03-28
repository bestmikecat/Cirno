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
        
        // ✅ 修改：PLAYER_STATE_STARTED 时直接解冻，不依赖 setAudio 返回值
        if (event == PLAYER_STATE_STARTED) {
            set.add(interfaceId);
            
            // ✅ 关键修改：无条件解冻（不管 setAudio 返回值）
            appRecord.getAppState().setAudio(true);
            Log.i("★ 应用 " + appRecord.getPackageNameWithUser() + " 开始播放音频 (STARTED)");
            FreezerService.thaw(appRecord);
            Log.i("  → 已解冻应用");
            
        } else if (event == PLAYER_STATE_PAUSED) {
            set.remove(interfaceId);
            
            // ✅ 修改：PAUSED 时检查是否还有其他音频
            if (set.isEmpty()) {
                appRecord.getAppState().setAudio(false);
                Log.i("★ 应用 " + appRecord.getPackageNameWithUser() + " 暂停播放 (PAUSED)");
                FreezerHandler.sendFreezeMessageIgnoreMessages(appRecord, 6000);
                Log.i("  → 6秒后冻结");
            }
            
        } else if (event == PLAYER_STATE_STOPPED || event == PLAYER_STATE_RELEASED) {
            set.remove(interfaceId);
            
            // ✅ 修改：STOPPED/RELEASED 时也检查
            if (set.isEmpty()) {
                appRecord.getAppState().setAudio(false);
                Log.i("★ 应用 " + appRecord.getPackageNameWithUser() + 
                      " 停止播放 (" + eventToString(event) + ")");
                FreezerHandler.sendFreezeMessageIgnoreMessages(appRecord, 6000);
                Log.i("  → 6秒后冻结");
            }
            
        } else if (event == PLAYER_STATE_IDLE) {
            set.remove(interfaceId);
            
            if (set.isEmpty()) {
                appRecord.getAppState().setAudio(false);
                Log.i("★ 应用 " + appRecord.getPackageNameWithUser() + " 空闲 (IDLE)");
                FreezerHandler.sendFreezeMessageIgnoreMessages(appRecord, 6000);
                Log.i("  → 6秒后冻结");
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