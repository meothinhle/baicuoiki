package com.warehouse.dao;

import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

public class SystemLogDAO {
    private static final Logger logger = Logger.getLogger(SystemLogDAO.class.getName());

    public void log(String username, String action, String target, String detail, boolean success, String ipAddress) {
        String sql = "INSERT INTO system_logs (username, action, target, detail, status, ip_address) VALUES (?,?,?,?,?,?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, action);
            ps.setString(3, target);
            ps.setString(4, detail);
            ps.setString(5, success ? "SUCCESS" : "FAIL");
            ps.setString(6, ipAddress);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.warning("Lỗi ghi log: " + e.getMessage());
        }
    }

    public List<Map<String, Object>> findAll(int limit) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT * FROM system_logs ORDER BY created_at DESC LIMIT ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", rs.getInt("id"));
                    row.put("username", rs.getString("username"));
                    row.put("action", rs.getString("action"));
                    row.put("target", rs.getString("target"));
                    row.put("detail", rs.getString("detail"));
                    row.put("status", rs.getString("status"));
                    row.put("ip_address", rs.getString("ip_address"));
                    row.put("created_at", rs.getString("created_at"));
                    list.add(row);
                }
            }
        } catch (SQLException e) {
            logger.severe("Lỗi findAll logs: " + e.getMessage());
        }
        return list;
    }
}
