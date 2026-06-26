package nep.timeline.cirno.socket;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import nep.timeline.cirno.GlobalVars;
import nep.timeline.cirno.log.Log;
import nep.timeline.cirno.provide.ApplicationBinderFacade;
import nep.timeline.cirno.provide.FrozenStateBinderFacade;
import nep.timeline.cirno.provide.StatusBinderFacade;

public final class SocketClient {
    private static final SocketClient INSTANCE = new SocketClient();
    private static final Gson gson = new Gson();
    private static final long CONNECT_TIMEOUT_MS = 3000L;
    private static final long REQUEST_TIMEOUT_MS = 5000L;
    private static final long RECONNECT_BASE_DELAY_MS = 500L;
    private static final long RECONNECT_MAX_DELAY_MS = 5000L;

    private final AtomicLong requestIdGenerator = new AtomicLong(0);
    private final ReentrantLock lock = new ReentrantLock();
    private Socket socket;
    private InputStream in;
    private OutputStream out;
    private volatile boolean connected = false;
    private volatile long lastConnectFailedAtMs = 0;
    private volatile int cachedServerPort = 0;

    private SocketClient() {
    }

    public static SocketClient getInstance() {
        return INSTANCE;
    }

    public boolean isConnected() {
        return connected;
    }

    private synchronized void ensureConnected() throws IOException {
        if (connected && socket != null && !socket.isClosed()) {
            return;
        }
        long now = System.currentTimeMillis();
        long elapsed = now - lastConnectFailedAtMs;
        if (lastConnectFailedAtMs > 0 && elapsed < RECONNECT_BASE_DELAY_MS) {
            try {
                Thread.sleep(RECONNECT_BASE_DELAY_MS - elapsed);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
        connect();
    }

    private void connect() throws IOException {
        close();
        String token = readToken();
        if (token == null || token.isEmpty()) {
            throw new IOException("no auth token available");
        }

        int port = tryConnect(token);
        if (port < 0) {
            lastConnectFailedAtMs = System.currentTimeMillis();
            throw new IOException("failed to connect to any port");
        }
        cachedServerPort = port;
    }

    private int tryConnect(String token) throws IOException {
        if (cachedServerPort > 0) {
            int port = trySingleConnect(cachedServerPort, token);
            if (port > 0) return port;
        }
        for (int i = 0; i < SocketProtocol.MAX_PORT_ATTEMPTS; i++) {
            int port = SocketProtocol.PORT + i;
            int result = trySingleConnect(port, token);
            if (result > 0) return result;
        }
        return -1;
    }

    private int trySingleConnect(int port, String token) {
        Socket s = null;
        try {
            s = new Socket();
            s.connect(new InetSocketAddress(SocketProtocol.HOST, port), (int) CONNECT_TIMEOUT_MS);
            s.setSoTimeout((int) REQUEST_TIMEOUT_MS);
            InputStream sockIn = s.getInputStream();
            OutputStream sockOut = s.getOutputStream();

            long id = requestIdGenerator.incrementAndGet();
            JsonObject params = new JsonObject();
            params.addProperty("token", token);
            String handshake = SocketProtocol.createRequest(id, SocketProtocol.METHOD_HANDSHAKE, gson.toJson(params));
            SocketProtocol.writeMessage(sockOut, handshake);
            String response = SocketProtocol.readMessage(sockIn);
            if (response == null) {
                return -1;
            }
            String error = SocketProtocol.parseError(response);
            if (error != null) {
                return -1;
            }
            String result = SocketProtocol.parseResult(response);
            if (result == null || !result.contains("ok")) {
                return -1;
            }
            socket = s;
            in = sockIn;
            out = sockOut;
            connected = true;
            lastConnectFailedAtMs = 0;
            Log.i("SocketClient: connected to port " + port);
            return port;
        } catch (Exception e) {
            if (s != null) {
                try { s.close(); } catch (IOException ignored) {}
            }
            return -1;
        }
    }

    private static String readToken() {
        File tokenFile = new File(GlobalVars.CONFIG_DIR, "cirno_hook.token");
        if (tokenFile.exists()) {
            try {
                String token = new String(Files.readAllBytes(tokenFile.toPath()), java.nio.charset.StandardCharsets.UTF_8).trim();
                if (!token.isEmpty()) {
                    return token;
                }
            } catch (IOException ignored) {
            }
        }
        return null;
    }

    public synchronized void close() {
        connected = false;
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
            socket = null;
        }
        in = null;
        out = null;
    }

    private String sendRequest(String method, String paramsJson) throws IOException {
        lock.lock();
        try {
            ensureConnected();
            long id = requestIdGenerator.incrementAndGet();
            String request = SocketProtocol.createRequest(id, method, paramsJson);
            SocketProtocol.writeMessage(out, request);
            String response = SocketProtocol.readMessage(in);
            if (response == null) {
                close();
                throw new IOException("null response");
            }
            String error = SocketProtocol.parseError(response);
            if (error != null) {
                throw new IOException("server error: " + error);
            }
            return SocketProtocol.parseResult(response);
        } catch (IOException e) {
            close();
            throw e;
        } finally {
            lock.unlock();
        }
    }

    public StatusBinderFacade getStatusBinder() {
        return statusFacade;
    }

    public ApplicationBinderFacade getApplicationBinder() {
        return applicationFacade;
    }

    public FrozenStateBinderFacade getFrozenStateBinder() {
        return frozenStateFacade;
    }

    private final StatusBinderFacade statusFacade = new StatusBinderFacade() {
        @Override
        public String getSignal(String key) {
            try {
                String params = "{\"key\":" + gson.toJson(key) + "}";
                String result = sendRequest("getSignal", params);
                return gson.fromJson(result, String.class);
            } catch (Exception e) {
                Log.w("SocketClient: getSignal failed", e);
                return "";
            }
        }

        @Override
        public boolean isPacketAvailable() {
            try {
                String result = sendRequest("isPacketAvailable", null);
                return gson.fromJson(result, Boolean.class);
            } catch (Exception e) {
                Log.w("SocketClient: isPacketAvailable failed", e);
                return false;
            }
        }

        @Override
        public String getHookVersion() {
            try {
                String result = sendRequest("getHookVersion", null);
                return gson.fromJson(result, String.class);
            } catch (Exception e) {
                Log.w("SocketClient: getHookVersion failed", e);
                return null;
            }
        }
    };

    private final ApplicationBinderFacade applicationFacade = new ApplicationBinderFacade() {
        @Override
        public List<String> getRunningApplication() {
            try {
                String result = sendRequest("getRunningApplication", null);
                return gson.fromJson(result, new TypeToken<List<String>>() {}.getType());
            } catch (Exception e) {
                Log.w("SocketClient: getRunningApplication failed", e);
                return java.util.Collections.emptyList();
            }
        }

        @Override
        public String getProcessesForApp(String packageName, int userId) {
            try {
                String params = "{\"packageName\":" + gson.toJson(packageName) + ",\"userId\":" + userId + "}";
                String result = sendRequest("getProcessesForApp", params);
                return gson.fromJson(result, String.class);
            } catch (Exception e) {
                Log.w("SocketClient: getProcessesForApp failed", e);
                return "[]";
            }
        }

        @Override
        public String getNetworkSpeed(String packageName, int userId) {
            try {
                String params = "{\"packageName\":" + gson.toJson(packageName) + ",\"userId\":" + userId + "}";
                String result = sendRequest("getNetworkSpeed", params);
                return gson.fromJson(result, String.class);
            } catch (Exception e) {
                Log.w("SocketClient: getNetworkSpeed failed", e);
                return "{\"rx\":0,\"tx\":0}";
            }
        }
    };

    private final FrozenStateBinderFacade frozenStateFacade = new FrozenStateBinderFacade() {
        @Override
        public String isFrozen(String packageName, int userId) {
            try {
                String params = "{\"packageName\":" + gson.toJson(packageName) + ",\"userId\":" + userId + "}";
                String result = sendRequest("isFrozen", params);
                return gson.fromJson(result, String.class);
            } catch (Exception e) {
                Log.w("SocketClient: isFrozen failed", e);
                return "NOT_FROZEN[UNKNOWN]";
            }
        }

        @Override
        public List<String> getFrozenStates(List<String> apps) {
            try {
                String params = "{\"apps\":" + gson.toJson(apps) + "}";
                String result = sendRequest("getFrozenStates", params);
                return gson.fromJson(result, new TypeToken<List<String>>() {}.getType());
            } catch (Exception e) {
                Log.w("SocketClient: getFrozenStates failed", e);
                return java.util.Collections.emptyList();
            }
        }
    };
}
