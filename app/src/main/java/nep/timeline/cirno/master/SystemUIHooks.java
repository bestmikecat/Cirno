package nep.timeline.cirno.master;

import nep.timeline.cirno.hooks.systemui.SystemUIApplicationHook;
import nep.timeline.cirno.hooks.systemui.tile.TileClickHook;

public class SystemUIHooks {
    public static void start(ClassLoader classLoader) {
        TileClickHook tileClickHook = new TileClickHook(classLoader);
        if (tileClickHook.isHooked()) {
            new SystemUIApplicationHook(classLoader);
        }
    }
}
