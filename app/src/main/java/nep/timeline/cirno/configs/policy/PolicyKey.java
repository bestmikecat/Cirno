package nep.timeline.cirno.configs.policy;

public class PolicyKey {
    public static String of(String packageName, int userId) {
        return packageName + "#" + userId;
    }
}
