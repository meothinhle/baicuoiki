package com.warehouse.service;

/**
 * Lưu thông tin phiên đăng nhập của user hiện tại
 */
public class ClientSession {
    private static ClientSession instance;

    private String token;
    private String username;
    private String fullName;
    private String role;
    private int userId;

    private ClientSession() {}

    public static ClientSession getInstance() {
        if (instance == null) instance = new ClientSession();
        return instance;
    }

    public void login(int userId, String username, String fullName, String role, String token) {
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.token = token;
    }

    public void logout() {
        this.token = null;
        this.username = null;
        this.fullName = null;
        this.role = null;
        this.userId = 0;
    }

    public boolean isLoggedIn() { return token != null; }
    public boolean isAdmin() { return "ADMIN".equals(role); }

    public String getToken() { return token; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getRole() { return role; }
    public int getUserId() { return userId; }
}
