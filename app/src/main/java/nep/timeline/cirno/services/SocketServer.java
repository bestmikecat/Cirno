package nep.timeline.cirno.services;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import nep.timeline.cirno.log.Log;
import nep.timeline.cirno.socket.SocketProtocol;

public final class SocketServer {
    private static final ExecutorService serverExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Cirno-SocketServer");
        t.setDaemon(true);
        return t;
    });
    private static final ExecutorService clientExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "Cirno-SocketHandler");
        t.setDaemon(true);
        return t;
    });
    private static final AtomicBoolean running = new AtomicBoolean(false);
    private static final Gson gson = new Gson();

    private SocketServer() {
    }

    public static void start() {
        if (running.compareAndSet(false, true)) {
            serverExecutor.execute(SocketServer::run);
            Log.i("SocketServer: starting");
        }
    }

    public static void stop() {
        running.set(false);
        serverExecutor.shutdownNow();
        clientExecutor.shutdownNow();
    }

    private static void run() {
        ServerSocket server = null;
        try {
            server = new ServerSocket(SocketProtocol.PORT, 50, InetAddress.getByName(SocketProtocol.HOST));
            Log.i("SocketServer: listening on " + SocketProtocol.HOST + ":" + SocketProtocol.PORT);

            while (running.get()) {
                try {
                    Socket client = server.accept();
                    if (client != null) {
                        clientExecutor.execute(() -> handleClient(client));
                    }
                } catch (IOException e) {
                    if (running.get()) {
                        Log.w("SocketServer: accept failed", e);
                    }
                }
            }
        } catch (IOException e) {
            Log.e("SocketServer: failed to start", e);
        } finally {
            running.set(false);
            if (server != null) {
                try {
                    server.close();
                } catch (IOException ignored) {
                }
            }
            Log.i("SocketServer: stopped");
        }
    }

    private static void handleClient(Socket client) {
        try {
            client.setSoTimeout(0);
            InputStream in = client.getInputStream();
            OutputStream out = client.getOutputStream();

            while (running.get() && !client.isClosed()) {
                String json = SocketProtocol.readMessage(in);
                if (json == null) {
                    break;
                }
                String response = dispatch(json);
                if (response != null) {
                    SocketProtocol.writeMessage(out, response);
                }
            }
        } catch (IOException e) {
            if (running.get()) {
                Log.w("SocketServer: client handler error", e);
            }
        } finally {
            try {
                client.close();
            } catch (IOException ignored) {
            }
        }
    }

    private static String dispatch(String requestJson) {
        long id;
        try {
            id = SocketProtocol.parseId(requestJson);
        } catch (Exception e) {
            return SocketProtocol.createErrorResponse(0, "invalid request");
        }

        String method = SocketProtocol.parseMethod(requestJson);
        if (method == null) {
            return SocketProtocol.createErrorResponse(id, "missing method");
        }

        JsonObject params = SocketProtocol.parseParams(requestJson);

        try {
            return switch (method) {
                case "getSignal" -> handleGetSignal(id, params);
                case "isPacketAvailable" -> handleIsPacketAvailable(id);
                case "getHookVersion" -> handleGetHookVersion(id);
                case "getRunningApplication" -> handleGetRunningApplication(id);
                case "getProcessesForApp" -> handleGetProcessesForApp(id, params);
                case "getNetworkSpeed" -> handleGetNetworkSpeed(id, params);
                case "isFrozen" -> handleIsFrozen(id, params);
                case "getFrozenStates" -> handleGetFrozenStates(id, params);
                default -> SocketProtocol.createErrorResponse(id, "unknown method: " + method);
            };
        } catch (Exception e) {
            return SocketProtocol.createErrorResponse(id, e.getMessage());
        }
    }

    private static String handleGetSignal(long id, JsonObject params) {
        String key = SocketProtocol.getStringParam(params, "key", "");
        String result = StatusBinderHub.getSignal(key);
        return SocketProtocol.createResponse(id, gson.toJsonTree(result));
    }

    private static String handleIsPacketAvailable(long id) {
        boolean result = StatusBinderHub.statusBinder.isPacketAvailable();
        return SocketProtocol.createResponse(id, gson.toJsonTree(result));
    }

    private static String handleGetHookVersion(long id) {
        String result = StatusBinderHub.statusBinder.getHookVersion();
        return SocketProtocol.createResponse(id, gson.toJsonTree(result));
    }

    private static String handleGetRunningApplication(long id) {
        List<String> result = MonitorBinderHub.applicationBinder.getRunningApplication();
        return SocketProtocol.createResponse(id, gson.toJsonTree(result));
    }

    private static String handleGetProcessesForApp(long id, JsonObject params) {
        String packageName = SocketProtocol.getStringParam(params, "packageName", "");
        int userId = SocketProtocol.getIntParam(params, "userId", 0);
        String result = MonitorBinderHub.applicationBinder.getProcessesForApp(packageName, userId);
        return SocketProtocol.createResponse(id, gson.toJsonTree(result));
    }

    private static String handleGetNetworkSpeed(long id, JsonObject params) {
        String packageName = SocketProtocol.getStringParam(params, "packageName", "");
        int userId = SocketProtocol.getIntParam(params, "userId", 0);
        String result = MonitorBinderHub.applicationBinder.getNetworkSpeed(packageName, userId);
        return SocketProtocol.createResponse(id, gson.toJsonTree(result));
    }

    private static String handleIsFrozen(long id, JsonObject params) {
        String packageName = SocketProtocol.getStringParam(params, "packageName", "");
        int userId = SocketProtocol.getIntParam(params, "userId", 0);
        String result = MonitorBinderHub.frozenStateBinder.isFrozen(packageName, userId);
        return SocketProtocol.createResponse(id, gson.toJsonTree(result));
    }

    private static String handleGetFrozenStates(long id, JsonObject params) {
        JsonArray appsArray = params.getAsJsonArray("apps");
        List<String> apps = new java.util.ArrayList<>();
        if (appsArray != null) {
            for (int i = 0; i < appsArray.size(); i++) {
                apps.add(appsArray.get(i).getAsString());
            }
        }
        List<String> result = MonitorBinderHub.frozenStateBinder.getFrozenStates(apps);
        return SocketProtocol.createResponse(id, gson.toJsonTree(result));
    }
}
