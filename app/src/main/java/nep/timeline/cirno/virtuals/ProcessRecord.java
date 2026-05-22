package nep.timeline.cirno.virtuals;

import android.content.pm.ApplicationInfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import de.robv.android.xposed.XposedHelpers;
import nep.timeline.cirno.entity.AppRecord;
import nep.timeline.cirno.services.AppService;

public class ProcessRecord {
    private final Object instance;
    private final int userId;
    private final int runningUid;
    private final ApplicationInfo applicationInfo;
    private final int uid;
    private final String packageName;
    private final String processName;
    private AppRecord appRecord;
    private volatile boolean frozen;
    private volatile long cachedRssKb = 0L;

    public ProcessRecord(Object instance) {
        this.instance = instance;
        this.userId = XposedHelpers.getIntField(instance, "userId");
        this.runningUid = XposedHelpers.getIntField(instance, "uid");
        this.applicationInfo = (ApplicationInfo) XposedHelpers.getObjectField(instance, "info");
        this.uid = applicationInfo.uid;
        this.packageName = applicationInfo.packageName;
        this.processName = (String) XposedHelpers.getObjectField(instance, "processName");
        this.appRecord = AppService.get(packageName, userId);
    }

    public int getPid() {
        return (int) XposedHelpers.getObjectField(instance, "mPid");
    }

    public boolean isDeathProcess() {
        return getPid() <= 0;
    }

    public AppRecord getAppRecord() {
        if (appRecord == null)
            appRecord = AppService.get(packageName, userId);
        return appRecord;
    }

    public int getUserId() {
        return userId;
    }

    public int getRunningUid() {
        return runningUid;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getProcessName() {
        return processName;
    }

    public boolean isFrozen() {
        return frozen;
    }

    public void setFrozen(boolean frozen) {
        this.frozen = frozen;
    }

    public long getCachedRssKb() {
        return cachedRssKb;
    }

    public void updateCachedRss() {
        if (isDeathProcess()) {
            cachedRssKb = 0L;
            return;
        }
        Long rss = readRssFromStatusFile("/proc/" + getPid() + "/status");
        cachedRssKb = rss == null ? 0L : rss;
    }

    private static Long readRssFromStatusFile(String path) {
        File file = new File(path);
        if (!file.exists() || !file.canRead()) {
            return null;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (!trimmed.startsWith("VmRSS:")) {
                    continue;
                }
                String[] parts = trimmed.split("\\s+");
                if (parts.length >= 2) {
                    return Long.parseLong(parts[1]);
                }
            }
        } catch (IOException | NumberFormatException ignored) {
        }
        return null;
    }
}
