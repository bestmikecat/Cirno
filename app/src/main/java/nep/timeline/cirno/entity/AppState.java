package nep.timeline.cirno.entity;

import android.os.IBinder;

import java.util.HashSet;
import java.util.Set;

public class AppState {
    private final AppRecord parent;
    private final Set<IBinder> activities = new HashSet<>();
    private final Set<IBinder> locationListeners = new HashSet<>();
    private final Set<Integer> interfaceIds = new HashSet<>();
    private final Set<Integer> recodingIds = new HashSet<>();
    private volatile boolean visible = false;
    private volatile boolean location = false;
    private volatile boolean audio = false;
    private volatile boolean recording = false;
    private volatile boolean vpn = false;
    private volatile boolean networkActive = false;

    public AppState(AppRecord appRecord) {
        this.parent = appRecord;
    }

    public synchronized boolean setVisible(boolean value) {
        if (visible == value)
            return false;
        visible = value;
        return true;
    }

    public synchronized boolean setLocation(boolean value) {
        if (location == value)
            return false;
        location = value;
        return true;
    }

    public synchronized boolean setAudio(boolean value) {
        if (audio == value)
            return false;
        audio = value;
        return true;
    }

    public synchronized boolean setRecording(boolean value) {
        if (recording == value)
            return false;
        recording = value;
        return true;
    }

    public synchronized boolean setVpn(boolean value) {
        if (vpn == value)
            return false;
        vpn = value;
        return true;
    }

    public synchronized boolean setNetworkActive(boolean value) {
        if (networkActive == value)
            return false;
        networkActive = value;
        return true;
    }

    public synchronized boolean addActivity(IBinder activity) {
        if (!activities.add(activity) || visible)
            return false;
        visible = true;
        return true;
    }

    public synchronized boolean removeActivity(IBinder activity) {
        if (!activities.remove(activity) || !activities.isEmpty() || !visible)
            return false;
        visible = false;
        return true;
    }

    public synchronized boolean addLocationListener(IBinder listener) {
        if (!locationListeners.add(listener) || location)
            return false;
        location = true;
        return true;
    }

    public synchronized boolean removeLocationListener(IBinder listener) {
        if (!locationListeners.remove(listener) || !locationListeners.isEmpty() || !location)
            return false;
        location = false;
        return true;
    }

    public synchronized boolean addAudioInterface(int interfaceId) {
        if (!interfaceIds.add(interfaceId) || audio)
            return false;
        audio = true;
        return true;
    }

    public synchronized boolean removeAudioInterface(int interfaceId) {
        if (!interfaceIds.remove(interfaceId) || !interfaceIds.isEmpty() || !audio)
            return false;
        audio = false;
        return true;
    }

    public synchronized boolean addRecordingId(int recordingId) {
        if (!recodingIds.add(recordingId) || recording)
            return false;
        recording = true;
        return true;
    }

    public synchronized boolean removeRecordingId(int recordingId) {
        if (!recodingIds.remove(recordingId) || !recodingIds.isEmpty() || !recording)
            return false;
        recording = false;
        return true;
    }

    public boolean isVisible() {
        return visible;
    }

    public boolean isLocation() {
        return location;
    }

    public boolean isAudio() {
        return audio;
    }

    public boolean isRecording() {
        return recording;
    }

    public boolean isVpn() {
        return vpn;
    }

    public boolean isNetworkActive() {
        return networkActive;
    }
}
