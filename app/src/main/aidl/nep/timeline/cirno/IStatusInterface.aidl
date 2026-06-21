package nep.timeline.cirno;

interface IStatusInterface {
    String getSignal(String key);
    boolean isPacketAvailable();
    String getHookVersion();
}
