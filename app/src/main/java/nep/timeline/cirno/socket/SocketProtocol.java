package nep.timeline.cirno.socket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public final class SocketProtocol {
    public static final String HOST = "127.0.0.1";
    public static final int PORT = 60192;
    public static final int MAX_PORT_ATTEMPTS = 10;
    public static final long BIND_RETRY_DELAY_MS = 2000L;
    public static final int CLIENT_SO_TIMEOUT_MS = 60_000;
    public static final String METHOD_HANDSHAKE = "handshake";
    private static final int MAX_MESSAGE_SIZE = 512 * 1024;
    private static final Gson gson = new Gson();

    private SocketProtocol() {
    }

    public static void writeMessage(OutputStream out, String json) throws IOException {
        byte[] payload = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (payload.length > MAX_MESSAGE_SIZE) {
            throw new IOException("Message too large: " + payload.length);
        }
        byte[] header = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(payload.length).array();
        out.write(header);
        out.write(payload);
        out.flush();
    }

    public static String readMessage(InputStream in) throws IOException {
        byte[] header = readExact(in, 4);
        if (header == null) {
            return null;
        }
        int length = ByteBuffer.wrap(header).order(ByteOrder.BIG_ENDIAN).getInt();
        if (length <= 0 || length > MAX_MESSAGE_SIZE) {
            throw new IOException("Invalid message length: " + length);
        }
        byte[] payload = readExact(in, length);
        if (payload == null) {
            return null;
        }
        return new String(payload, java.nio.charset.StandardCharsets.UTF_8);
    }

    private static byte[] readExact(InputStream in, int count) throws IOException {
        byte[] buf = new byte[count];
        int offset = 0;
        while (offset < count) {
            int n = in.read(buf, offset, count - offset);
            if (n < 0) {
                return null;
            }
            offset += n;
        }
        return buf;
    }

    public static String createRequest(long id, String method, String paramsJson) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", id);
        obj.addProperty("method", method);
        if (paramsJson != null) {
            obj.add("params", JsonParser.parseString(paramsJson).getAsJsonObject());
        } else {
            obj.add("params", new JsonObject());
        }
        return gson.toJson(obj);
    }

    public static String createResponse(long id, JsonElement result) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", id);
        obj.add("result", result);
        return gson.toJson(obj);
    }

    public static String createErrorResponse(long id, String error) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", id);
        obj.addProperty("error", error);
        return gson.toJson(obj);
    }

    public static long parseId(String json) {
        return JsonParser.parseString(json).getAsJsonObject().get("id").getAsLong();
    }

    public static String parseMethod(String json) {
        JsonElement method = JsonParser.parseString(json).getAsJsonObject().get("method");
        return method != null ? method.getAsString() : null;
    }

    public static JsonObject parseParams(String json) {
        JsonElement params = JsonParser.parseString(json).getAsJsonObject().get("params");
        return params != null && params.isJsonObject() ? params.getAsJsonObject() : new JsonObject();
    }

    public static String getStringParam(JsonObject params, String key, String defaultValue) {
        JsonElement elem = params.get(key);
        return elem != null && !elem.isJsonNull() ? elem.getAsString() : defaultValue;
    }

    public static int getIntParam(JsonObject params, String key, int defaultValue) {
        JsonElement elem = params.get(key);
        return elem != null && !elem.isJsonNull() ? elem.getAsInt() : defaultValue;
    }

    public static String parseResult(String json) {
        JsonElement result = JsonParser.parseString(json).getAsJsonObject().get("result");
        return result != null ? result.toString() : null;
    }

    public static String parseError(String json) {
        JsonElement error = JsonParser.parseString(json).getAsJsonObject().get("error");
        return error != null ? error.getAsString() : null;
    }
}
