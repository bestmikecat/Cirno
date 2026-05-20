package nep.timeline.cirno.binders;

public interface ApplicationInterface extends android.os.IInterface {
    class Default implements ApplicationInterface {
        @Override
        public java.util.List<java.lang.String> getRunningApplication() throws android.os.RemoteException {
            return null;
        }

        @Override
        public String getProcessesForApp(String packageName, int userId) throws android.os.RemoteException {
            return null;
        }

        @Override
        public String getNetworkSpeed(String packageName, int userId) throws android.os.RemoteException {
            return null;
        }

        @Override
        public android.os.IBinder asBinder() {
            return null;
        }
    }

    abstract class Stub extends android.os.Binder implements ApplicationInterface {
        public Stub() {
            this.attachInterface(this, DESCRIPTOR);
        }

        public static ApplicationInterface asInterface(android.os.IBinder obj) {
            if (obj == null) {
                return null;
            }
            android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if ((iin != null) && (iin instanceof ApplicationInterface)) {
                return (ApplicationInterface) iin;
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
            if (code == TRANSACTION_getRunningApplication) {
                java.util.List<java.lang.String> result = this.getRunningApplication();
                reply.writeNoException();
                reply.writeStringList(result);
                return true;
            }
            if (code == TRANSACTION_getProcessesForApp) {
                String packageName = data.readString();
                int userId = data.readInt();
                String result = this.getProcessesForApp(packageName, userId);
                reply.writeNoException();
                reply.writeString(result);
                return true;
            }
            if (code == TRANSACTION_getNetworkSpeed) {
                String packageName = data.readString();
                int userId = data.readInt();
                String result = this.getNetworkSpeed(packageName, userId);
                reply.writeNoException();
                reply.writeString(result);
                return true;
            }
            return super.onTransact(code, data, reply, flags);
        }

        protected static class Proxy implements ApplicationInterface {
            private final android.os.IBinder mRemote;

            public Proxy(android.os.IBinder remote) {
                mRemote = remote;
            }

            @Override
            public android.os.IBinder asBinder() {
                return mRemote;
            }

            @Override
            public java.util.List<java.lang.String> getRunningApplication() throws android.os.RemoteException {
                android.os.Parcel data = android.os.Parcel.obtain();
                android.os.Parcel reply = android.os.Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(Stub.TRANSACTION_getRunningApplication, data, reply, 0);
                    reply.readException();
                    return reply.createStringArrayList();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public String getProcessesForApp(String packageName, int userId) throws android.os.RemoteException {
                android.os.Parcel data = android.os.Parcel.obtain();
                android.os.Parcel reply = android.os.Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeString(packageName);
                    data.writeInt(userId);
                    mRemote.transact(Stub.TRANSACTION_getProcessesForApp, data, reply, 0);
                    reply.readException();
                    return reply.readString();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public String getNetworkSpeed(String packageName, int userId) throws android.os.RemoteException {
                android.os.Parcel data = android.os.Parcel.obtain();
                android.os.Parcel reply = android.os.Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeString(packageName);
                    data.writeInt(userId);
                    mRemote.transact(Stub.TRANSACTION_getNetworkSpeed, data, reply, 0);
                    reply.readException();
                    return reply.readString();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }
        }

        static final int TRANSACTION_getRunningApplication = android.os.IBinder.FIRST_CALL_TRANSACTION;
        static final int TRANSACTION_getProcessesForApp = android.os.IBinder.FIRST_CALL_TRANSACTION + 1;
        static final int TRANSACTION_getNetworkSpeed = android.os.IBinder.FIRST_CALL_TRANSACTION + 2;
    }

    String DESCRIPTOR = "nep.timeline.cirno.binders.ApplicationInterface";

    java.util.List<java.lang.String> getRunningApplication() throws android.os.RemoteException;

    String getProcessesForApp(String packageName, int userId) throws android.os.RemoteException;

    String getNetworkSpeed(String packageName, int userId) throws android.os.RemoteException;
}
