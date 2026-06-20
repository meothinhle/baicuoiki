package com.warehouse.dao;

import com.warehouse.model.Category;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class CategoryDAO {
    private static final Logger logger = Logger.getLogger(CategoryDAO.class.getName());

    public List<Category> findAll() {
        List<Category> list = new ArrayList<>();
        String sql = "SELECT * FROM categories WHERE status='ACTIVE' ORDER BY name";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            logger.severe("Lỗi findAll categories: " + e.getMessage());
        }
        return list;
    }

    public Category findById(int id) {
        String sql = "SELECT * FROM categories WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            logger.severe("Lỗi findById category: " + e.getMessage());
        }
        return null;
    }

    public boolean insert(Category c) {
        String sql = "INSERT INTO categories (code, name, description, status) VALUES (?,?,?,?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, c.getCode());
            ps.setString(2, c.getName());
            ps.setString(3, c.getDescription());
            ps.setString(4, "ACTIVE");
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.severe("Lỗi insert category: " + e.getMessage());
            return false;
        }
    }

    public boolean update(Category c) {
        String sql = "UPDATE categories SET name=?, description=?, status=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, c.getName());
            ps.setString(2, c.getDescription());
            ps.setString(3, c.getStatus());
            ps.setInt(4, c.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.severe("Lỗi update category: " + e.getMessage());
            return false;
        }
    }

    public boolean delete(int id) {
        String sql = "UPDATE categories SET status='INACTIVE' WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.severe("Lỗi delete category: " + e.getMessage());
            return false;
        }
    }

    public List<Category> search(String keyword) {
        List<Category> list = new ArrayList<>();
        String sql = "SELECT * FROM categories WHERE status='ACTIVE' AND (code LIKE ? OR name LIKE ?) ORDER BY name";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String kw = "%" + keyword + "%";
            ps.setString(1, kw); ps.setString(2, kw);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.severe("Lỗi search category: " + e.getMessage());
        }
        return list;
    }

    private Category mapRow(ResultSet rs) throws SQLException {
        Category c = new Category();
        c.setId(rs.getInt("id"));
        c.setCode(rs.getString("code"));
        c.setName(rs.getString("name"));
        c.setDescription(rs.getString("description"));
        c.setStatus(rs.getString("status"));
        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null) c.setCreatedAt(ca.toLocalDateTime());
        return c;
    }
}
