package com.warehouse.service;

import com.google.gson.JsonObject;

/**
 * Lớp tiện ích gọi API, tự động truyền token của session hiện tại
 */
public class ApiClient {
    private static ApiClient instance;
    private final ServerConnection conn = new ServerConnection();

    private ApiClient() {}

    public static ApiClient getInstance() {
        if (instance == null) instance = new ApiClient();
        return instance;
    }

    public JsonObject call(String action, Object data) {
        String token = ClientSession.getInstance().getToken();
        return conn.send(action, data, token);
    }

    public JsonObject call(String action) {
        return call(action, null);
    }

    public boolean ensureConnected() {
        return conn.isConnected() || conn.connect();
    }

    public void disconnect() {
        conn.disconnect();
    }
}
