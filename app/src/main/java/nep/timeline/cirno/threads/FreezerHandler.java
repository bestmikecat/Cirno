package nep.timeline.cirno.threads;

import android.os.Handler;
import android.os.Message;
import nep.timeline.cirno.GlobalVars;
import nep.timeline.cirno.entity.AppRecord;

public class FreezerHandler {
    public static final Handler handler = new FreezerMessageHandler(Handlers.makeLooper("Freezer"));

    public static void removeAppMessage(AppRecord appRecord) {
        if (handler.hasMessages(0, appRecord))
            handler.removeMessages(0, appRecord);
    }

    public static void sendFreezeMessage(AppRecord appRecord, long delay) {
        if (handler.hasMessages(0, appRecord))
            return;

        sendFreezeMessageIgnoreMessages(appRecord, delay);
    }

    public static void sendFreezeMessageIgnoreMessages(AppRecord appRecord, long delay) {
        removeAppMessage(appRecord);
        private static final long FreezeDelayNum = 1000 * GlobalVars.globalSettings.freezeDelay;

        Message obtain = handler.obtainMessage(0, appRecord);
        if (FreezeDelayNum < 1)
            handler.sendMessage(obtain);
        else
            handler.sendMessageDelayed(obtain, FreezeDelayNum);
    }
}
