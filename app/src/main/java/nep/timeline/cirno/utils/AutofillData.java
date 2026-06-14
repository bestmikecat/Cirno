package nep.timeline.cirno.utils;

import java.util.HashMap;
import java.util.Map;

import nep.timeline.cirno.entity.AppRecord;

public class AutofillData {
    public static volatile Object instance;
    public static volatile AppRecord currentAutofillApp;
    private static final Map<String, AppRecord> activeSessions = new HashMap<>();

    public static String makeSessionKey(int userId, int sessionId) {
        return userId + ":" + sessionId;
    }

    public static synchronized boolean putSession(int userId, int sessionId, AppRecord appRecord) {
        return activeSessions.put(makeSessionKey(userId, sessionId), appRecord) == null;
    }

    public static synchronized AppRecord removeSession(int userId, int sessionId) {
        return activeSessions.remove(makeSessionKey(userId, sessionId));
    }

    public static synchronized boolean hasActiveSession(AppRecord appRecord) {
        return activeSessions.containsValue(appRecord);
    }

    public static synchronized int getActiveSessionCount(AppRecord appRecord) {
        int count = 0;
        for (AppRecord record : activeSessions.values()) {
            if (record.equals(appRecord)) {
                count++;
            }
        }
        return count;
    }

    public static synchronized int getSessionCount() {
        return activeSessions.size();
    }
}
