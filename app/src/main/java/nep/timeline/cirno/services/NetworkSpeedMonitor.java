package nep.timeline.cirno.services;

import java.util.concurrent.ConcurrentHashMap;

import de.robv.android.xposed.XposedHelpers;
import nep.timeline.cirno.GlobalVars;
import nep.timeline.cirno.configs.checkers.AppConfigs;
import nep.timeline.cirno.entity.AppRecord;
import nep.timeline.cirno.entity.AppState;
import nep.timeline.cirno.log.Log;
import nep.timeline.cirno.threads.Handlers;

public class NetworkSpeedMonitor {
    private static volatile Object sService;
    private static volatile ClassLoader sClassLoader;
    private static volatile boolean sMonitoring = false;

    private static final ConcurrentHashMap<Integer, long[]> sSnapshots = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, long[]> sSpeedCache = new ConcurrentHashMap<>();

    public static void setInstance(Object service, ClassLoader classLoader) {
        sService = service;
        sClassLoader = classLoader;
        startMonitoring();
    }

    private static void startMonitoring() {
        if (sMonitoring || sService == null)
            return;
        sMonitoring = true;
        Log.i("NetworkSpeedMonitor started");
        Handlers.network.postDelayed(NetworkSpeedMonitor::poll, 1000);
    }

    private static void poll() {
        if (sService == null)
            return;
        try {
            long now = System.currentTimeMillis();
            int threshold = GlobalVars.globalSettings != null ? GlobalVars.globalSettings.networkSpeedThreshold : 102400;
            for (AppRecord record : AppService.getAllRecordsSnapshot()) {
                if (record == null)
                    continue;
                if (!AppConfigs.isNetworkSpeedAllowed(record.getPackageName(), record.getUserId())) {
                    sSnapshots.remove(record.getUid());
                    sSpeedCache.remove(record.getUid());
                    record.getAppState().setNetworkActive(false);
                    continue;
                }
                readAndCalculate(record.getUid(), now, threshold, record.getAppState());
            }
        } catch (Throwable e) {
            Log.e("NetworkSpeedMonitor poll error", e);
        }
        Handlers.network.postDelayed(NetworkSpeedMonitor::poll, 1000);
    }

    private static void readAndCalculate(int uid, long now, int threshold, AppState appState) {
        try {
            Object stats = XposedHelpers.callMethod(sService,
                    "readNetworkStatsUidDetail", uid, null, -1);
            long totalRx = 0;
            long totalTx = 0;

            int size = (int) XposedHelpers.callMethod(stats, "size");
            Object entry = null;
            for (int i = 0; i < size; i++) {
                entry = XposedHelpers.callMethod(stats, "getValues", i, entry);
                totalRx += (long) XposedHelpers.getObjectField(entry, "rxBytes");
                totalTx += (long) XposedHelpers.getObjectField(entry, "txBytes");
            }

            long[] prev = sSnapshots.get(uid);
            long speed = 0;
            if (prev != null) {
                long deltaTime = now - prev[2];
                if (deltaTime > 0) {
                    long rxSpeed = (totalRx - (long) prev[0]) * 1000 / deltaTime;
                    long txSpeed = (totalTx - (long) prev[1]) * 1000 / deltaTime;
                    if (rxSpeed < 0) rxSpeed = 0;
                    if (txSpeed < 0) txSpeed = 0;
                    sSpeedCache.put(uid, new long[]{rxSpeed, txSpeed});
                    speed = rxSpeed + txSpeed;
                }
            }
            sSnapshots.put(uid, new long[]{totalRx, totalTx, now});
            appState.setNetworkActive(speed > threshold);
        } catch (Throwable e) {
            Log.d("readNetworkStatsUidDetail failed for uid=" + uid);
        }
    }

    public static long[] getSpeed(int uid) {
        long[] speed = sSpeedCache.get(uid);
        return speed != null ? speed : new long[]{0, 0};
    }
}
