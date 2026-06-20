package com.warehouse.dao;

import com.warehouse.model.Product;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class ProductDAO {
    private static final Logger logger = Logger.getLogger(ProductDAO.class.getName());

    public List<Product> findAll() {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT p.*, c.name as category_name FROM products p LEFT JOIN categories c ON p.category_id = c.id WHERE p.status='ACTIVE' ORDER BY p.code";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            logger.severe("Lỗi findAll products: " + e.getMessage());
        }
        return list;
    }

    public Product findById(int id) {
        String sql = "SELECT p.*, c.name as category_name FROM products p LEFT JOIN categories c ON p.category_id = c.id WHERE p.id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            logger.severe("Lỗi findById product: " + e.getMessage());
        }
        return null;
    }

    public Product findByCode(String code) {
        String sql = "SELECT p.*, c.name as category_name FROM products p LEFT JOIN categories c ON p.category_id = c.id WHERE p.code=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            logger.severe("Lỗi findByCode product: " + e.getMessage());
        }
        return null;
    }

    public boolean insert(Product p) {
        String sql = "INSERT INTO products (code, name, category_id, brand, unit, cost_price, sell_price, quantity, min_quantity, description, status) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getCode());
            ps.setString(2, p.getName());
            ps.setInt(3, p.getCategoryId());
            ps.setString(4, p.getBrand());
            ps.setString(5, p.getUnit());
            ps.setBigDecimal(6, p.getCostPrice());
            ps.setBigDecimal(7, p.getSellPrice());
            ps.setInt(8, p.getQuantity());
            ps.setInt(9, p.getMinQuantity());
            ps.setString(10, p.getDescription());
            ps.setString(11, p.getStatus() != null ? p.getStatus() : "ACTIVE");
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.severe("Lỗi insert product: " + e.getMessage());
            return false;
        }
    }

    public boolean update(Product p) {
        String sql = "UPDATE products SET name=?, category_id=?, brand=?, unit=?, cost_price=?, sell_price=?, quantity=?, min_quantity=?, description=?, status=?, updated_at=NOW() WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getName());
            ps.setInt(2, p.getCategoryId());
            ps.setString(3, p.getBrand());
            ps.setString(4, p.getUnit());
            ps.setBigDecimal(5, p.getCostPrice());
            ps.setBigDecimal(6, p.getSellPrice());
            ps.setInt(7, p.getQuantity());
            ps.setInt(8, p.getMinQuantity());
            ps.setString(9, p.getDescription());
            ps.setString(10, p.getStatus());
            ps.setInt(11, p.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.severe("Lỗi update product: " + e.getMessage());
            return false;
        }
    }

    public boolean updateQuantity(Connection conn, int productId, int delta) throws SQLException {
        String sql = "UPDATE products SET quantity = quantity + ?, updated_at=NOW() WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, delta);
            ps.setInt(2, productId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) {
        String sql = "UPDATE products SET status='INACTIVE', updated_at=NOW() WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.severe("Lỗi delete product: " + e.getMessage());
            return false;
        }
    }

    public List<Product> search(String keyword) {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT p.*, c.name as category_name FROM products p LEFT JOIN categories c ON p.category_id = c.id " +
                "WHERE p.status='ACTIVE' AND (p.code LIKE ? OR p.name LIKE ? OR p.brand LIKE ? OR c.name LIKE ?) ORDER BY p.code";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String kw = "%" + keyword + "%";
            ps.setString(1, kw); ps.setString(2, kw); ps.setString(3, kw); ps.setString(4, kw);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.severe("Lỗi search product: " + e.getMessage());
        }
        return list;
    }

    public List<Product> findByCategory(int categoryId) {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT p.*, c.name as category_name FROM products p LEFT JOIN categories c ON p.category_id = c.id WHERE p.category_id=? AND p.status='ACTIVE' ORDER BY p.code";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.severe("Lỗi findByCategory: " + e.getMessage());
        }
        return list;
    }

    public List<Product> findLowStock() {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT p.*, c.name as category_name FROM products p LEFT JOIN categories c ON p.category_id = c.id WHERE p.status='ACTIVE' AND p.quantity <= p.min_quantity ORDER BY p.quantity ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            logger.severe("Lỗi findLowStock: " + e.getMessage());
        }
        return list;
    }

    private Product mapRow(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setId(rs.getInt("id"));
        p.setCode(rs.getString("code"));
        p.setName(rs.getString("name"));
        p.setCategoryId(rs.getInt("category_id"));
        p.setCategoryName(rs.getString("category_name"));
        p.setBrand(rs.getString("brand"));
        p.setUnit(rs.getString("unit"));
        p.setCostPrice(rs.getBigDecimal("cost_price"));
        p.setSellPrice(rs.getBigDecimal("sell_price"));
        p.setQuantity(rs.getInt("quantity"));
        p.setMinQuantity(rs.getInt("min_quantity"));
        p.setDescription(rs.getString("description"));
        p.setStatus(rs.getString("status"));
        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null) p.setCreatedAt(ca.toLocalDateTime());
        Timestamp ua = rs.getTimestamp("updated_at");
        if (ua != null) p.setUpdatedAt(ua.toLocalDateTime());
        return p;
    }
}
