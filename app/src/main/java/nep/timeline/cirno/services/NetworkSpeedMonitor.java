package nep.timeline.cirno.services;

import android.net.TrafficStats;
import android.os.IBinder;
import android.os.ServiceManager;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

import de.robv.android.xposed.XposedHelpers;
import nep.timeline.cirno.GlobalVars;
import nep.timeline.cirno.configs.checkers.AppConfigs;
import nep.timeline.cirno.entity.AppRecord;
import nep.timeline.cirno.entity.AppState;
import nep.timeline.cirno.log.Log;
import nep.timeline.cirno.threads.Handlers;

public class NetworkSpeedMonitor {
    private static volatile IBinder sNetStatsBinder;
    private static volatile boolean sMonitoring = false;
    private static volatile boolean sUseTrafficStatsFallback = false;

    private static final ConcurrentHashMap<Integer, long[]> sSnapshots = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, long[]> sSpeedCache = new ConcurrentHashMap<>();

    public static void init() {
        if (sMonitoring)
            return;
        try {
            sNetStatsBinder = ServiceManager.getService("netstats");
        } catch (Throwable e) {
            Log.e("NetworkSpeedMonitor: failed to get netstats service", e);
        }

        if (sNetStatsBinder != null) {
            Log.i("NetworkSpeedMonitor: using NetworkStatsService via ServiceManager");
            sUseTrafficStatsFallback = false;
        } else {
            Log.i("NetworkSpeedMonitor: netstats service not found, fallback to TrafficStats");
            sUseTrafficStatsFallback = true;
        }

        sMonitoring = true;
        Handlers.network.postDelayed(NetworkSpeedMonitor::poll, 1000);
    }

    private static void poll() {
        try {
            long now = System.currentTimeMillis();
            int threshold = GlobalVars.globalSettings != null ? GlobalVars.globalSettings.networkSpeedThreshold : 102400;
            int monitoredCount = 0;
            for (AppRecord record : AppService.getAllRecordsSnapshot()) {
                if (record == null)
                    continue;
                if (!AppConfigs.isNetworkSpeedAllowed(record.getPackageName(), record.getUserId())) {
                    sSnapshots.remove(record.getUid());
                    sSpeedCache.remove(record.getUid());
                    record.getAppState().setNetworkActive(false);
                    continue;
                }
                monitoredCount++;
                if (sUseTrafficStatsFallback) {
                    readAndCalculateViaTrafficStats(record.getUid(), now, threshold, record.getAppState());
                } else {
                    readAndCalculate(record.getUid(), now, threshold, record.getAppState());
                }
            }
            if (monitoredCount > 0) {
                Log.d("NetworkSpeedMonitor: polling via " + (sUseTrafficStatsFallback ? "TrafficStats" : "NetworkStatsService") + ", monitored UIDs=" + monitoredCount);
            }
        } catch (Throwable e) {
            Log.e("NetworkSpeedMonitor poll error", e);
        }
        Handlers.network.postDelayed(NetworkSpeedMonitor::poll, 1000);
    }

    private static void readAndCalculate(int uid, long now, int threshold, AppState appState) {
        try {
            Class<?> serviceClass = sNetStatsBinder.getClass();
            Method readMethod = serviceClass.getDeclaredMethod("readNetworkStatsUidDetail",
                    int.class, String[].class, int.class);
            readMethod.setAccessible(true);
            Object stats = readMethod.invoke(sNetStatsBinder, uid, null, -1);

            long totalRx = 0;
            long totalTx = 0;

            int size = (int) XposedHelpers.callMethod(stats, "size");
            Object entry = null;
            for (int i = 0; i < size; i++) {
                entry = XposedHelpers.callMethod(stats, "getValues", i, entry);
                totalRx += (long) XposedHelpers.getObjectField(entry, "rxBytes");
                totalTx += (long) XposedHelpers.getObjectField(entry, "txBytes");
            }

            calculateSpeed(uid, totalRx, totalTx, now, threshold, appState);
        } catch (Throwable e) {
            Log.d("NetworkStatsService failed for uid=" + uid + ", switching to TrafficStats fallback");
            sUseTrafficStatsFallback = true;
            readAndCalculateViaTrafficStats(uid, now, threshold, appState);
        }
    }

    private static void readAndCalculateViaTrafficStats(int uid, long now, int threshold, AppState appState) {
        try {
            long totalRx = TrafficStats.getUidRxBytes(uid);
            long totalTx = TrafficStats.getUidTxBytes(uid);
            calculateSpeed(uid, totalRx, totalTx, now, threshold, appState);
        } catch (Throwable e) {
            Log.d("TrafficStats failed for uid=" + uid);
        }
    }

    private static void calculateSpeed(int uid, long totalRx, long totalTx, long now, int threshold, AppState appState) {
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
    }

    public static long[] getSpeed(int uid) {
        long[] speed = sSpeedCache.get(uid);
        return speed != null ? speed : new long[]{0, 0};
    }
}
