package nep.timeline.cirno.services;

import android.os.Handler;

import nep.timeline.cirno.entity.AppRecord;
import nep.timeline.cirno.threads.Handlers;
import nep.timeline.cirno.virtuals.ProcessRecord;

public class RssUpdateService {
    private static final long UPDATE_INTERVAL_MS = 5000L;
    private static final Handler handler = new Handler(Handlers.log.getLooper());

    private static final Runnable updateTask = new Runnable() {
        @Override
        public void run() {
            for (AppRecord appRecord : AppService.getAllRecordsSnapshot()) {
                if (appRecord == null) continue;
                for (ProcessRecord pr : appRecord.getProcessRecords()) {
                    if (pr != null && !pr.isDeathProcess() && pr.isFrozen()) {
                        pr.updateCachedRss();
                    }
                }
            }
            handler.postDelayed(this, UPDATE_INTERVAL_MS);
        }
    };

    public static void start() {
        handler.postDelayed(updateTask, UPDATE_INTERVAL_MS);
    }
}
