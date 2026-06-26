package nep.timeline.cirno.provide;

import java.util.List;

public interface FrozenStateBinderFacade {
    String isFrozen(String packageName, int userId);
    List<String> getFrozenStates(List<String> apps);
}
