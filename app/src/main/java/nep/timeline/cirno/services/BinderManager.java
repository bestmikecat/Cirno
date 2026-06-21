package nep.timeline.cirno.services;

import nep.timeline.cirno.IBinderManager;
import nep.timeline.cirno.IStatusInterface;
import nep.timeline.cirno.IApplicationInterface;
import nep.timeline.cirno.IFrozenStateInterface;

public class BinderManager extends IBinderManager.Stub {
    private final IStatusInterface statusBinder;
    private final IApplicationInterface applicationBinder;
    private final IFrozenStateInterface frozenStateBinder;

    public BinderManager(IStatusInterface statusBinder,
                         IApplicationInterface applicationBinder,
                         IFrozenStateInterface frozenStateBinder) {
        this.statusBinder = statusBinder;
        this.applicationBinder = applicationBinder;
        this.frozenStateBinder = frozenStateBinder;
    }

    @Override
    public IStatusInterface getStatusBinder() {
        return statusBinder;
    }

    @Override
    public IApplicationInterface getApplicationBinder() {
        return applicationBinder;
    }

    @Override
    public IFrozenStateInterface getFrozenStateBinder() {
        return frozenStateBinder;
    }
}
