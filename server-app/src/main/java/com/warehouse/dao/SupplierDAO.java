package com.warehouse.dao;

import com.warehouse.model.Supplier;
import com.warehouse.security.AESUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class SupplierDAO {
    private static final Logger logger = Logger.getLogger(SupplierDAO.class.getName());

    public List<Supplier> findAll() {
        List<Supplier> list = new ArrayList<>();
        String sql = "SELECT * FROM suppliers WHERE status='ACTIVE' ORDER BY name";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            logger.severe("Lỗi findAll suppliers: " + e.getMessage());
        }
        return list;
    }

    public Supplier findById(int id) {
        String sql = "SELECT * FROM suppliers WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            logger.severe("Lỗi findById supplier: " + e.getMessage());
        }
        return null;
    }

    public boolean insert(Supplier s) {
        String sql = "INSERT INTO suppliers (code, name, contact, phone, email, address, status) VALUES (?,?,?,?,?,?,?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, s.getCode());
            ps.setString(2, s.getName());
            ps.setString(3, s.getContact());
            ps.setString(4, s.getPhone() != null ? AESUtil.encrypt(s.getPhone()) : null);
            ps.setString(5, s.getEmail() != null ? AESUtil.encrypt(s.getEmail()) : null);
            ps.setString(6, s.getAddress());
            ps.setString(7, "ACTIVE");
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.severe("Lỗi insert supplier: " + e.getMessage());
            return false;
        }
    }

    public boolean update(Supplier s) {
        String sql = "UPDATE suppliers SET name=?, contact=?, phone=?, email=?, address=?, status=?, updated_at=NOW() WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, s.getName());
            ps.setString(2, s.getContact());
            ps.setString(3, s.getPhone() != null ? AESUtil.encrypt(s.getPhone()) : null);
            ps.setString(4, s.getEmail() != null ? AESUtil.encrypt(s.getEmail()) : null);
            ps.setString(5, s.getAddress());
            ps.setString(6, s.getStatus());
            ps.setInt(7, s.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.severe("Lỗi update supplier: " + e.getMessage());
            return false;
        }
    }

    public boolean delete(int id) {
        String sql = "UPDATE suppliers SET status='INACTIVE', updated_at=NOW() WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.severe("Lỗi delete supplier: " + e.getMessage());
            return false;
        }
    }

    public List<Supplier> search(String keyword) {
        List<Supplier> list = new ArrayList<>();
        String sql = "SELECT * FROM suppliers WHERE status='ACTIVE' AND (code LIKE ? OR name LIKE ? OR contact LIKE ?) ORDER BY name";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String kw = "%" + keyword + "%";
            ps.setString(1, kw); ps.setString(2, kw); ps.setString(3, kw);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.severe("Lỗi search supplier: " + e.getMessage());
        }
        return list;
    }

    private Supplier mapRow(ResultSet rs) throws SQLException {
        Supplier s = new Supplier();
        s.setId(rs.getInt("id"));
        s.setCode(rs.getString("code"));
        s.setName(rs.getString("name"));
        s.setContact(rs.getString("contact"));
        String encPhone = rs.getString("phone");
        String encEmail = rs.getString("email");
        s.setPhone(encPhone != null ? AESUtil.decrypt(encPhone) : null);
        s.setEmail(encEmail != null ? AESUtil.decrypt(encEmail) : null);
        s.setAddress(rs.getString("address"));
        s.setStatus(rs.getString("status"));
        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null) s.setCreatedAt(ca.toLocalDateTime());
        return s;
    }
}
