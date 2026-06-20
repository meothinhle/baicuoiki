package com.warehouse.service;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.warehouse.util.GsonFactory;

import java.io.*;
import java.net.Socket;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Kết nối TCP đến server, gửi request và nhận response
 */
public class ServerConnection {
    private static final Logger logger = Logger.getLogger(ServerConnection.class.getName());

    private static String HOST = "localhost";
    private static int PORT = 9000;

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;

    private final Gson gson = GsonFactory.create();

    static {
        Properties props = new Properties();
        try (InputStream is = ServerConnection.class.getClassLoader().getResourceAsStream("client.properties")) {
            if (is != null) {
                props.load(is);
                HOST = props.getProperty("server.host", "localhost");
                PORT = Integer.parseInt(props.getProperty("server.port", "9000"));
            }
        } catch (Exception ignored) {}
    }

    public boolean connect() {
        try {
            socket = new Socket(HOST, PORT);
            // Không đặt timeout cố định: app desktop có thể idle lâu giữa
            // các thao tác của người dùng mà không có nghĩa là mất kết nối.
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            return true;
        } catch (IOException e) {
            logger.severe("Không thể kết nối server " + HOST + ":" + PORT + " - " + e.getMessage());
            return false;
        }
    }

    public void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    /**
     * Gửi request và nhận response từ server.
     * Tự động thử kết nối lại 1 lần nếu socket hiện tại đã "chết"
     * (ví dụ: do đã mở kết nối từ trước nhưng server đã đóng nó).
     */
    public JsonObject send(String action, Object data, String token) {
        if (!isConnected() && !connect()) {
            return errorResponse("Không thể kết nối server. Kiểm tra server đang chạy trên " + HOST + ":" + PORT);
        }
        JsonObject result = trySend(action, data, token);
        if (result.has("__retry__")) {
            // Socket cũ đã chết -> thử kết nối lại 1 lần
            if (!connect()) {
                return errorResponse("Không thể kết nối server. Kiểm tra server đang chạy trên " + HOST + ":" + PORT);
            }
            result = trySend(action, data, token);
            if (result.has("__retry__")) {
                return errorResponse("Mất kết nối server, vui lòng thử lại");
            }
        }
        return result;
    }

    private JsonObject trySend(String action, Object data, String token) {
        try {
            JsonObject req = new JsonObject();
            req.addProperty("action", action);
            req.addProperty("token", token != null ? token : "");
            if (data != null) req.add("data", gson.toJsonTree(data));
            else req.add("data", JsonNull.INSTANCE);

            writer.println(gson.toJson(req));
            if (writer.checkError()) throw new IOException("Không gửi được dữ liệu (socket đã đóng)");

            String response = reader.readLine();
            if (response == null) throw new IOException("Server đóng kết nối");
            return JsonParser.parseString(response).getAsJsonObject();
        } catch (IOException e) {
            logger.warning("Lỗi giao tiếp với server: " + e.getMessage());
            disconnect();
            JsonObject retry = new JsonObject();
            retry.addProperty("__retry__", true);
            return retry;
        }
    }

    private JsonObject errorResponse(String message) {
        JsonObject err = new JsonObject();
        err.addProperty("success", false);
        err.addProperty("message", message);
        return err;
    }

    public JsonObject send(String action, String token) {
        return send(action, null, token);
    }

    public static String getHost() { return HOST; }
    public static int getPort() { return PORT; }
}