package com.warehouse.service;

import com.warehouse.dao.SystemLogDAO;
import com.warehouse.dao.UserDAO;
import com.warehouse.model.User;
import com.warehouse.security.PasswordUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service xử lý đăng nhập, phân quyền, quản lý session
 */
public class AuthService {
    private final UserDAO userDAO = new UserDAO();
    private final SystemLogDAO logDAO = new SystemLogDAO();

    // Lưu token -> User (token = username đơn giản, production dùng JWT)
    private final Map<String, User> sessionMap = new ConcurrentHashMap<>();

    /**
     * Đăng nhập
     * @return User nếu thành công, null nếu thất bại
     * @throws Exception với thông báo lỗi cụ thể
     */
    public User login(String username, String password, String clientIp) throws Exception {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new Exception("Vui lòng nhập tên đăng nhập và mật khẩu");
        }

        User user = userDAO.findByUsername(username.trim());
        if (user == null) {
            logDAO.log(username, "LOGIN", "users", "Tên đăng nhập không tồn tại", false, clientIp);
            throw new Exception("Tên đăng nhập hoặc mật khẩu không đúng");
        }

        if ("LOCKED".equals(user.getStatus())) {
            logDAO.log(username, "LOGIN", "users", "Tài khoản bị khoá", false, clientIp);
            throw new Exception("Tài khoản đã bị khoá. Vui lòng liên hệ Admin");
        }

        if (!PasswordUtil.checkPassword(password, user.getPassword())) {
            userDAO.incrementFailCount(username);
            int remaining = 5 - user.getFailCount() - 1;
            logDAO.log(username, "LOGIN", "users", "Sai mật khẩu, còn " + remaining + " lần", false, clientIp);
            if (remaining <= 0) {
                throw new Exception("Sai mật khẩu quá nhiều lần. Tài khoản đã bị khoá");
            }
            throw new Exception("Sai mật khẩu. Còn " + remaining + " lần thử");
        }

        // Đăng nhập thành công
        userDAO.resetFailCount(username);
        user.setPassword(null);  // Không giữ password hash trong session

        // Tạo token đơn giản (production: dùng JWT)
        String token = username + "_" + System.currentTimeMillis();
        sessionMap.put(token, user);

        logDAO.log(username, "LOGIN", "users", "Đăng nhập thành công", true, clientIp);

        // Trả về user với token được set
        user.setPassword(token); // Dùng tạm field password để truyền token về client
        return user;
    }

    /**
     * Đăng xuất
     */
    public void logout(String token, String clientIp) {
        User user = sessionMap.remove(token);
        if (user != null) {
            logDAO.log(user.getUsername(), "LOGOUT", "users", "Đăng xuất", true, clientIp);
        }
    }

    /**
     * Kiểm tra token hợp lệ
     */
    public User validateToken(String token) {
        return sessionMap.get(token);
    }

    /**
     * Kiểm tra quyền Admin
     */
    public boolean isAdmin(String token) {
        User user = validateToken(token);
        return user != null && user.isAdmin();
    }

    /**
     * Lấy user từ token
     */
    public User getUserFromToken(String token) {
        return sessionMap.get(token);
    }
}
