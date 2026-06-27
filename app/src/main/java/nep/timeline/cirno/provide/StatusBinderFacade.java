package nep.timeline.cirno.provide;

public interface StatusBinderFacade {
    String getSignal(String key);
    String getStatusSnapshot();
    boolean isPacketAvailable();
    String getHookVersion();
}
