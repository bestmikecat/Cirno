package nep.timeline.cirno;

import nep.timeline.cirno.IStatusInterface;
import nep.timeline.cirno.IApplicationInterface;
import nep.timeline.cirno.IFrozenStateInterface;

interface IBinderManager {
    IStatusInterface getStatusBinder();
    IApplicationInterface getApplicationBinder();
    IFrozenStateInterface getFrozenStateBinder();
}
