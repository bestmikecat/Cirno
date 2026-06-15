package nep.timeline.cirno.utils;

import java.util.HashMap;
import java.util.Map;

import nep.timeline.cirno.entity.AppRecord;

public class CredentialData {
    private static final Map<String, AppRecord> activeSessions = new HashMap<>();

    public static String makeSessionKey(int userId, Object providerSession) {
        return userId + ":" + System.identityHashCode(providerSession);
    }

    public static synchronized boolean putSession(int userId, Object providerSession, AppRecord appRecord) {
        return activeSessions.put(makeSessionKey(userId, providerSession), appRecord) == null;
    }

    public static synchronized AppRecord removeSession(int userId, Object providerSession) {
        return activeSessions.remove(makeSessionKey(userId, providerSession));
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
