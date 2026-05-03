package nep.timeline.cirno.binders;

public interface FrozenStateInterface extends android.os.IInterface {
    class Default implements FrozenStateInterface {
        @Override
        public String isFrozen(String packageName, int userId) throws android.os.RemoteException {
            return null;
        }

        @Override
        public android.os.IBinder asBinder() {
            return null;
        }
    }

    abstract class Stub extends android.os.Binder implements FrozenStateInterface {
        public Stub() {
            this.attachInterface(this, DESCRIPTOR);
        }

        public static FrozenStateInterface asInterface(android.os.IBinder obj) {
            if (obj == null) {
                return null;
            }
            android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if ((iin != null) && (iin instanceof FrozenStateInterface)) {
                return (FrozenStateInterface) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public android.os.IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException {
            String descriptor = DESCRIPTOR;
            if (code >= android.os.IBinder.FIRST_CALL_TRANSACTION && code <= android.os.IBinder.LAST_CALL_TRANSACTION) {
                data.enforceInterface(descriptor);
            }
            if (code == INTERFACE_TRANSACTION) {
                reply.writeString(descriptor);
                return true;
            }
            if (code == TRANSACTION_isFrozen) {
                String packageName = data.readString();
                int userId = data.readInt();
                String result = this.isFrozen(packageName, userId);
                reply.writeNoException();
                reply.writeString(result);
                return true;
            }
            return super.onTransact(code, data, reply, flags);
        }

        protected static class Proxy implements FrozenStateInterface {
            private final android.os.IBinder mRemote;

            public Proxy(android.os.IBinder remote) {
                mRemote = remote;
            }

            @Override
            public android.os.IBinder asBinder() {
                return mRemote;
            }

            @Override
            public String isFrozen(String packageName, int userId) throws android.os.RemoteException {
                android.os.Parcel data = android.os.Parcel.obtain();
                android.os.Parcel reply = android.os.Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeString(packageName);
                    data.writeInt(userId);
                    mRemote.transact(Stub.TRANSACTION_isFrozen, data, reply, 0);
                    reply.readException();
                    return reply.readString();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }
        }

        static final int TRANSACTION_isFrozen = android.os.IBinder.FIRST_CALL_TRANSACTION;
    }

    String DESCRIPTOR = "nep.timeline.cirno.binders.FrozenStateInterface";

    String isFrozen(String packageName, int userId) throws android.os.RemoteException;
}
