package nep.timeline.cirno;

interface IApplicationInterface {
    List<String> getRunningApplication();
    String getProcessesForApp(String packageName, int userId);
    String getNetworkSpeed(String packageName, int userId);
}
