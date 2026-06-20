package com.warehouse.model;

import java.io.Serializable;

/**
 * Đối tượng Request gửi từ Client lên Server
 */
public class Request implements Serializable {
    private static final long serialVersionUID = 1L;

    private String action;   // Hành động: LOGIN, PRODUCT_FIND_ALL, ...
    private Object data;     // Dữ liệu kèm theo
    private String token;    // Token xác thực (username đang đăng nhập)

    public Request() {}

    public Request(String action) {
        this.action = action;
    }

    public Request(String action, Object data) {
        this.action = action;
        this.data = data;
    }

    public Request(String action, Object data, String token) {
        this.action = action;
        this.data = data;
        this.token = token;
    }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    @Override
    public String toString() {
        return "Request{action='" + action + "', token='" + token + "'}";
    }
}
