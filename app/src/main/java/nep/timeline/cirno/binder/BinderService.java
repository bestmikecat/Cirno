package nep.timeline.cirno.binder;

import nep.timeline.cirno.provide.StatusBinderFacade;
import nep.timeline.cirno.provide.ApplicationBinderFacade;
import nep.timeline.cirno.provide.FrozenStateBinderFacade;
import nep.timeline.cirno.socket.SocketClient;
import nep.timeline.cirno.log.Log;

public class BinderService {

    public static void register(android.content.Context appContext) {
        // Socket connection is lazy; no registration needed.
    }

    public static StatusBinderFacade getStatusBinder() {
        return SocketClient.getInstance().getStatusBinder();
    }

    public static ApplicationBinderFacade getApplicationBinder() {
        return SocketClient.getInstance().getApplicationBinder();
    }

    public static FrozenStateBinderFacade getFrozenStateBinder() {
        return SocketClient.getInstance().getFrozenStateBinder();
    }
}
