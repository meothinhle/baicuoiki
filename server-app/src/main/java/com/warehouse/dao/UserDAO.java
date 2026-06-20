package com.warehouse.dao;

import com.warehouse.model.User;
import com.warehouse.security.AESUtil;
import com.warehouse.security.PasswordUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class UserDAO {
    private static final Logger logger = Logger.getLogger(UserDAO.class.getName());
    private static final int MAX_FAIL_COUNT = 5;

    public List<User> findAll() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT id, username, full_name, email, phone, role, status, fail_count, created_at, updated_at FROM users ORDER BY id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRowSafe(rs));
            }
        } catch (SQLException e) {
            logger.severe("Lỗi findAll users: " + e.getMessage());
        }
        return list;
    }

    public User findById(int id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            logger.severe("Lỗi findById user: " + e.getMessage());
        }
        return null;
    }

    public User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            logger.severe("Lỗi findByUsername: " + e.getMessage());
        }
        return null;
    }

    public boolean insert(User user) {
        String sql = "INSERT INTO users (username, password, full_name, email, phone, role, status) VALUES (?,?,?,?,?,?,?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, PasswordUtil.hashPassword(user.getPassword()));
            ps.setString(3, user.getFullName());
            ps.setString(4, user.getEmail() != null ? AESUtil.encrypt(user.getEmail()) : null);
            ps.setString(5, user.getPhone() != null ? AESUtil.encrypt(user.getPhone()) : null);
            ps.setString(6, user.getRole());
            ps.setString(7, user.getStatus() != null ? user.getStatus() : "ACTIVE");
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.severe("Lỗi insert user: " + e.getMessage());
            return false;
        }
    }

    public boolean update(User user) {
        String sql = "UPDATE users SET full_name=?, email=?, phone=?, role=?, status=?, updated_at=NOW() WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getEmail() != null ? AESUtil.encrypt(user.getEmail()) : null);
            ps.setString(3, user.getPhone() != null ? AESUtil.encrypt(user.getPhone()) : null);
            ps.setString(4, user.getRole());
            ps.setString(5, user.getStatus());
            ps.setInt(6, user.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.severe("Lỗi update user: " + e.getMessage());
            return false;
        }
    }

    public boolean updatePassword(int userId, String newPassword) {
        String sql = "UPDATE users SET password=?, updated_at=NOW() WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, PasswordUtil.hashPassword(newPassword));
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.severe("Lỗi updatePassword: " + e.getMessage());
            return false;
        }
    }

    public boolean delete(int id) {
        // Không xoá thật, vô hiệu hoá
        String sql = "UPDATE users SET status='LOCKED', updated_at=NOW() WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.severe("Lỗi delete user: " + e.getMessage());
            return false;
        }
    }

    /**
     * Mở khoá tài khoản: đặt lại status ACTIVE và reset fail_count về 0
     * (để tài khoản không bị khoá lại ngay vì còn đếm sai số lần thử cũ).
     */
    public boolean unlock(int id) {
        String sql = "UPDATE users SET status='ACTIVE', fail_count=0, updated_at=NOW() WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.severe("Lỗi unlock user: " + e.getMessage());
            return false;
        }
    }

    public List<User> search(String keyword) {
        List<User> list = new ArrayList<>();
        String sql = "SELECT id, username, full_name, email, phone, role, status, fail_count, created_at, updated_at FROM users WHERE username LIKE ? OR full_name LIKE ? ORDER BY id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String kw = "%" + keyword + "%";
            ps.setString(1, kw);
            ps.setString(2, kw);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRowSafe(rs));
            }
        } catch (SQLException e) {
            logger.severe("Lỗi search user: " + e.getMessage());
        }
        return list;
    }

    public boolean incrementFailCount(String username) {
        String sql = "UPDATE users SET fail_count = fail_count + 1, status = CASE WHEN fail_count + 1 >= ? THEN 'LOCKED' ELSE status END WHERE username=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, MAX_FAIL_COUNT);
            ps.setString(2, username);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.severe("Lỗi incrementFailCount: " + e.getMessage());
            return false;
        }
    }

    public boolean resetFailCount(String username) {
        String sql = "UPDATE users SET fail_count = 0 WHERE username=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.severe("Lỗi resetFailCount: " + e.getMessage());
            return false;
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User u = mapRowSafe(rs);
        // Chỉ đọc password ở đây - dành cho các hàm dùng "SELECT *"
        // (findById, findByUsername), nơi cột password chắc chắn có mặt
        // trong ResultSet.
        u.setPassword(rs.getString("password"));
        return u;
    }

    /**
     * Map các field KHÔNG bao gồm password. Dùng cho findAll()/search(),
     * vì câu SELECT ở đó chỉ lấy danh sách cột cụ thể (không có "password")
     * để tránh trả password hash về client khi hiển thị danh sách. Trước
     * đây hàm này gọi mapRow() rồi xoá password sau, nhưng mapRow() lại cố
     * đọc rs.getString("password") - cột không tồn tại trong ResultSet của
     * findAll()/search() -> lỗi "Column 'password' not found".
     */
    private User mapRowSafe(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setUsername(rs.getString("username"));
        u.setFullName(rs.getString("full_name"));
        // Giải mã email, phone
        String encEmail = rs.getString("email");
        String encPhone = rs.getString("phone");
        u.setEmail(encEmail != null ? AESUtil.decrypt(encEmail) : null);
        u.setPhone(encPhone != null ? AESUtil.decrypt(encPhone) : null);
        u.setRole(rs.getString("role"));
        u.setStatus(rs.getString("status"));
        u.setFailCount(rs.getInt("fail_count"));
        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null) u.setCreatedAt(ca.toLocalDateTime());
        return u;
    }
}