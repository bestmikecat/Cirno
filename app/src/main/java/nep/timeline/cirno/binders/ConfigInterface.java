package nep.timeline.cirno.binders;

public interface ConfigInterface extends android.os.IInterface {
    class Default implements ConfigInterface {
        @Override
        public String getGlobalSettingsJson() throws android.os.RemoteException {
            return "";
        }

        @Override
        public String getApplicationSettingsJson() throws android.os.RemoteException {
            return "";
        }

        @Override
        public boolean setGlobalSettingsJson(String json) throws android.os.RemoteException {
            return false;
        }

        @Override
        public boolean setApplicationSettingsJson(String json) throws android.os.RemoteException {
            return false;
        }

        @Override
        public String getLastError() throws android.os.RemoteException {
            return "";
        }

        @Override
        public boolean setSignal(String key, String value) throws android.os.RemoteException {
            return false;
        }

        @Override
        public String getSignal(String key) throws android.os.RemoteException {
            return "";
        }

        @Override
        public java.util.List<java.lang.String> getManagedAppKeys() throws android.os.RemoteException {
            return new java.util.ArrayList<>();
        }

        @Override
        public String getModuleVersion() throws android.os.RemoteException {
            return "";
        }

        @Override
        public String getLogContent() throws android.os.RemoteException {
            return "";
        }

        @Override
        public String getLogContentPage(int startLine, int lineCount) throws android.os.RemoteException {
            return "";
        }

        @Override
        public android.os.IBinder asBinder() {
            return null;
        }
    }

    abstract class Stub extends android.os.Binder implements ConfigInterface {
        public Stub() {
            this.attachInterface(this, DESCRIPTOR);
        }

        public static ConfigInterface asInterface(android.os.IBinder obj) {
            if (obj == null) {
                return null;
            }
            android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if ((iin != null) && (iin instanceof ConfigInterface)) {
                return (ConfigInterface) iin;
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
                case TRANSACTION_getGlobalSettingsJson: {
                    String result = this.getGlobalSettingsJson();
                    reply.writeNoException();
                    reply.writeString(result);
                    return true;
                }
                case TRANSACTION_getApplicationSettingsJson: {
                    String result = this.getApplicationSettingsJson();
                    reply.writeNoException();
                    reply.writeString(result);
                    return true;
                }
                case TRANSACTION_setGlobalSettingsJson: {
                    String json = data.readString();
                    boolean result = this.setGlobalSettingsJson(json);
                    reply.writeNoException();
                    reply.writeInt(result ? 1 : 0);
                    return true;
                }
                case TRANSACTION_setApplicationSettingsJson: {
                    String json = data.readString();
                    boolean result = this.setApplicationSettingsJson(json);
                    reply.writeNoException();
                    reply.writeInt(result ? 1 : 0);
                    return true;
                }
                case TRANSACTION_getLastError: {
                    String result = this.getLastError();
                    reply.writeNoException();
                    reply.writeString(result);
                    return true;
                }
                case TRANSACTION_setSignal: {
                    String key = data.readString();
                    String value = data.readString();
                    boolean result = this.setSignal(key, value);
                    reply.writeNoException();
                    reply.writeInt(result ? 1 : 0);
                    return true;
                }
                case TRANSACTION_getSignal: {
                    String key = data.readString();
                    String result = this.getSignal(key);
                    reply.writeNoException();
                    reply.writeString(result);
                    return true;
                }
                case TRANSACTION_getManagedAppKeys: {
                    java.util.List<java.lang.String> result = this.getManagedAppKeys();
                    reply.writeNoException();
                    reply.writeStringList(result);
                    return true;
                }
                case TRANSACTION_getModuleVersion: {
                    String result = this.getModuleVersion();
                    reply.writeNoException();
                    reply.writeString(result);
                    return true;
                }
                case TRANSACTION_getLogContent: {
                    String result = this.getLogContent();
                    reply.writeNoException();
                    reply.writeString(result);
                    return true;
                }
                case TRANSACTION_getLogContentPage: {
                    int startLine = data.readInt();
                    int lineCount = data.readInt();
                    String result = this.getLogContentPage(startLine, lineCount);
                    reply.writeNoException();
                    reply.writeString(result);
                    return true;
                }
            }
            return super.onTransact(code, data, reply, flags);
        }

        protected static class Proxy implements ConfigInterface {
            private final android.os.IBinder mRemote;

            public Proxy(android.os.IBinder remote) {
                mRemote = remote;
            }

            @Override
            public android.os.IBinder asBinder() {
                return mRemote;
            }

            @Override
            public String getGlobalSettingsJson() throws android.os.RemoteException {
                android.os.Parcel data = android.os.Parcel.obtain();
                android.os.Parcel reply = android.os.Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(Stub.TRANSACTION_getGlobalSettingsJson, data, reply, 0);
                    reply.readException();
                    return reply.readString();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public String getApplicationSettingsJson() throws android.os.RemoteException {
                android.os.Parcel data = android.os.Parcel.obtain();
                android.os.Parcel reply = android.os.Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(Stub.TRANSACTION_getApplicationSettingsJson, data, reply, 0);
                    reply.readException();
                    return reply.readString();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public boolean setGlobalSettingsJson(String json) throws android.os.RemoteException {
                android.os.Parcel data = android.os.Parcel.obtain();
                android.os.Parcel reply = android.os.Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeString(json);
                    mRemote.transact(Stub.TRANSACTION_setGlobalSettingsJson, data, reply, 0);
                    reply.readException();
                    return reply.readInt() != 0;
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public boolean setApplicationSettingsJson(String json) throws android.os.RemoteException {
                android.os.Parcel data = android.os.Parcel.obtain();
                android.os.Parcel reply = android.os.Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeString(json);
                    mRemote.transact(Stub.TRANSACTION_setApplicationSettingsJson, data, reply, 0);
                    reply.readException();
                    return reply.readInt() != 0;
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public String getLastError() throws android.os.RemoteException {
                android.os.Parcel data = android.os.Parcel.obtain();
                android.os.Parcel reply = android.os.Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(Stub.TRANSACTION_getLastError, data, reply, 0);
                    reply.readException();
                    return reply.readString();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public boolean setSignal(String key, String value) throws android.os.RemoteException {
                android.os.Parcel data = android.os.Parcel.obtain();
                android.os.Parcel reply = android.os.Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeString(key);
                    data.writeString(value);
                    mRemote.transact(Stub.TRANSACTION_setSignal, data, reply, 0);
                    reply.readException();
                    return reply.readInt() != 0;
                } finally {
                    reply.recycle();
                    data.recycle();
                }
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
            public java.util.List<java.lang.String> getManagedAppKeys() throws android.os.RemoteException {
                android.os.Parcel data = android.os.Parcel.obtain();
                android.os.Parcel reply = android.os.Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(Stub.TRANSACTION_getManagedAppKeys, data, reply, 0);
                    reply.readException();
                    return reply.createStringArrayList();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public String getModuleVersion() throws android.os.RemoteException {
                android.os.Parcel data = android.os.Parcel.obtain();
                android.os.Parcel reply = android.os.Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(Stub.TRANSACTION_getModuleVersion, data, reply, 0);
                    reply.readException();
                    return reply.readString();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public String getLogContent() throws android.os.RemoteException {
                android.os.Parcel data = android.os.Parcel.obtain();
                android.os.Parcel reply = android.os.Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(Stub.TRANSACTION_getLogContent, data, reply, 0);
                    reply.readException();
                    return reply.readString();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public String getLogContentPage(int startLine, int lineCount) throws android.os.RemoteException {
                android.os.Parcel data = android.os.Parcel.obtain();
                android.os.Parcel reply = android.os.Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeInt(startLine);
                    data.writeInt(lineCount);
                    mRemote.transact(Stub.TRANSACTION_getLogContentPage, data, reply, 0);
                    reply.readException();
                    return reply.readString();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }
        }

        static final int TRANSACTION_getGlobalSettingsJson = android.os.IBinder.FIRST_CALL_TRANSACTION;
        static final int TRANSACTION_getApplicationSettingsJson = android.os.IBinder.FIRST_CALL_TRANSACTION + 1;
        static final int TRANSACTION_setGlobalSettingsJson = android.os.IBinder.FIRST_CALL_TRANSACTION + 2;
        static final int TRANSACTION_setApplicationSettingsJson = android.os.IBinder.FIRST_CALL_TRANSACTION + 3;
        static final int TRANSACTION_getLastError = android.os.IBinder.FIRST_CALL_TRANSACTION + 4;
        static final int TRANSACTION_setSignal = android.os.IBinder.FIRST_CALL_TRANSACTION + 5;
        static final int TRANSACTION_getSignal = android.os.IBinder.FIRST_CALL_TRANSACTION + 6;
        static final int TRANSACTION_getManagedAppKeys = android.os.IBinder.FIRST_CALL_TRANSACTION + 7;
        static final int TRANSACTION_getModuleVersion = android.os.IBinder.FIRST_CALL_TRANSACTION + 8;
        static final int TRANSACTION_getLogContent = android.os.IBinder.FIRST_CALL_TRANSACTION + 9;
        static final int TRANSACTION_getLogContentPage = android.os.IBinder.FIRST_CALL_TRANSACTION + 10;
    }

    String DESCRIPTOR = "nep.timeline.cirno.binders.ConfigInterface";

    String getGlobalSettingsJson() throws android.os.RemoteException;

    String getApplicationSettingsJson() throws android.os.RemoteException;

    boolean setGlobalSettingsJson(String json) throws android.os.RemoteException;

    boolean setApplicationSettingsJson(String json) throws android.os.RemoteException;

    String getLastError() throws android.os.RemoteException;

    boolean setSignal(String key, String value) throws android.os.RemoteException;

    String getSignal(String key) throws android.os.RemoteException;

    java.util.List<java.lang.String> getManagedAppKeys() throws android.os.RemoteException;

    String getModuleVersion() throws android.os.RemoteException;

    String getLogContent() throws android.os.RemoteException;

    String getLogContentPage(int startLine, int lineCount) throws android.os.RemoteException;
}
