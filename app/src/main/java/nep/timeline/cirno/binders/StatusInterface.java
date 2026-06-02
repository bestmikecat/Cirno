package nep.timeline.cirno.binders;

public interface StatusInterface extends android.os.IInterface {
    class Default implements StatusInterface {
        @Override
        public String getSignal(String key) throws android.os.RemoteException {
            return "";
        }

        @Override
        public boolean isReKernelAvailable() throws android.os.RemoteException {
            return false;
        }

        @Override
        public String getHookVersion() throws android.os.RemoteException {
            return "";
        }

        @Override
        public android.os.IBinder asBinder() {
            return null;
        }
    }

    abstract class Stub extends android.os.Binder implements StatusInterface {
        public Stub() {
            this.attachInterface(this, DESCRIPTOR);
        }

        public static StatusInterface asInterface(android.os.IBinder obj) {
            if (obj == null) {
                return null;
            }
            android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if ((iin != null) && (iin instanceof StatusInterface)) {
                return (StatusInterface) iin;
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
            switch (code) {
                case TRANSACTION_getSignal: {
                    String key = data.readString();
                    String result = this.getSignal(key);
                    reply.writeNoException();
                    reply.writeString(result);
                    return true;
                }
                case TRANSACTION_isReKernelAvailable: {
                    boolean result = this.isReKernelAvailable();
                    reply.writeNoException();
                    reply.writeInt(result ? 1 : 0);
                    return true;
                }
                case TRANSACTION_getHookVersion: {
                    String result = this.getHookVersion();
                    reply.writeNoException();
                    reply.writeString(result);
                    return true;
                }
            }
            return super.onTransact(code, data, reply, flags);
        }

        protected static class Proxy implements StatusInterface {
            private final android.os.IBinder mRemote;

            public Proxy(android.os.IBinder remote) {
                mRemote = remote;
            }

            @Override
            public android.os.IBinder asBinder() {
                return mRemote;
            }

            @Override
            public String getSignal(String key) throws android.os.RemoteException {
                android.os.Parcel data = android.os.Parcel.obtain();
                android.os.Parcel reply = android.os.Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeString(key);
                    mRemote.transact(Stub.TRANSACTION_getSignal, data, reply, 0);
                    reply.readException();
                    return reply.readString();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public boolean isReKernelAvailable() throws android.os.RemoteException {
                android.os.Parcel data = android.os.Parcel.obtain();
                android.os.Parcel reply = android.os.Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(Stub.TRANSACTION_isReKernelAvailable, data, reply, 0);
                    reply.readException();
                    return reply.readInt() != 0;
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public String getHookVersion() throws android.os.RemoteException {
                android.os.Parcel data = android.os.Parcel.obtain();
                android.os.Parcel reply = android.os.Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(Stub.TRANSACTION_getHookVersion, data, reply, 0);
                    reply.readException();
                    return reply.readString();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }
        }

        static final int TRANSACTION_getSignal = android.os.IBinder.FIRST_CALL_TRANSACTION;
        static final int TRANSACTION_isReKernelAvailable = android.os.IBinder.FIRST_CALL_TRANSACTION + 1;
        static final int TRANSACTION_getHookVersion = android.os.IBinder.FIRST_CALL_TRANSACTION + 2;
    }

    String DESCRIPTOR = "nep.timeline.cirno.binders.StatusInterface";

    String getSignal(String key) throws android.os.RemoteException;

    boolean isReKernelAvailable() throws android.os.RemoteException;

    String getHookVersion() throws android.os.RemoteException;
}
