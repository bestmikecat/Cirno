package nep.timeline.cirno;

interface IFrozenStateInterface {
    String isFrozen(String packageName, int userId);
    List<String> getFrozenStates(in List<String> apps);
}
