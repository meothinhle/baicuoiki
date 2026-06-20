package com.warehouse.model;

import java.io.Serializable;

/**
 * Đối tượng Response trả từ Server về Client
 */
public class Response implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean success;
    private String message;
    private Object data;
    private int totalCount;    // Dùng cho phân trang

    public Response() {}

    public Response(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public Response(boolean success, String message, Object data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public static Response ok(String message, Object data) {
        return new Response(true, message, data);
    }

    public static Response ok(String message) {
        return new Response(true, message);
    }

    public static Response fail(String message) {
        return new Response(false, message);
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }

    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
}
