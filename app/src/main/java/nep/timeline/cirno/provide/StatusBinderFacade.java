package nep.timeline.cirno.provide;

public interface StatusBinderFacade {
    String getSignal(String key);
    boolean isPacketAvailable();
    String getHookVersion();
}
