package com.warehouse.dao;

import com.warehouse.model.DashboardStats;

import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

public class DashboardDAO {
    private static final Logger logger = Logger.getLogger(DashboardDAO.class.getName());

    public DashboardStats getStats() {
        DashboardStats stats = new DashboardStats();
        try (Connection conn = DatabaseConnection.getConnection()) {
            stats.setTotalProducts(count(conn, "SELECT COUNT(*) FROM products WHERE status='ACTIVE'"));
            stats.setLowStockProducts(count(conn, "SELECT COUNT(*) FROM products WHERE status='ACTIVE' AND quantity <= min_quantity"));
            stats.setTotalSuppliers(count(conn, "SELECT COUNT(*) FROM suppliers WHERE status='ACTIVE'"));
            stats.setTotalCategories(count(conn, "SELECT COUNT(*) FROM categories WHERE status='ACTIVE'"));
            stats.setTotalImportOrders(count(conn, "SELECT COUNT(*) FROM import_orders WHERE status='COMPLETED'"));
            stats.setTotalExportOrders(count(conn, "SELECT COUNT(*) FROM export_orders WHERE status='COMPLETED'"));

            // Tổng giá trị nhập/xuất
            stats.setTotalImportValue(sumDecimal(conn, "SELECT COALESCE(SUM(total_amount),0) FROM import_orders WHERE status='COMPLETED'"));
            stats.setTotalExportValue(sumDecimal(conn, "SELECT COALESCE(SUM(total_amount),0) FROM export_orders WHERE status='COMPLETED'"));

            // Sản phẩm theo danh mục
            stats.setProductsByCategory(getProductsByCategory(conn));

            // Tồn kho thấp
            stats.setLowStockList(getLowStockList(conn));

        } catch (SQLException e) {
            logger.severe("Lỗi getStats: " + e.getMessage());
        }
        return stats;
    }

    private int count(Connection conn, String sql) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    private java.math.BigDecimal sumDecimal(Connection conn, String sql) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getBigDecimal(1);
        }
        return java.math.BigDecimal.ZERO;
    }

    private Map<String, Integer> getProductsByCategory(Connection conn) throws SQLException {
        Map<String, Integer> map = new LinkedHashMap<>();
        String sql = "SELECT c.name, COUNT(p.id) as cnt FROM categories c LEFT JOIN products p ON p.category_id=c.id AND p.status='ACTIVE' WHERE c.status='ACTIVE' GROUP BY c.id, c.name ORDER BY cnt DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                map.put(rs.getString("name"), rs.getInt("cnt"));
            }
        }
        return map;
    }

    private List<Map<String, Object>> getLowStockList(Connection conn) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT p.code, p.name, p.quantity, p.min_quantity, c.name as category_name FROM products p LEFT JOIN categories c ON p.category_id=c.id WHERE p.status='ACTIVE' AND p.quantity <= p.min_quantity ORDER BY p.quantity ASC LIMIT 10";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("code", rs.getString("code"));
                row.put("name", rs.getString("name"));
                row.put("quantity", rs.getInt("quantity"));
                row.put("minQuantity", rs.getInt("min_quantity"));
                row.put("categoryName", rs.getString("category_name"));
                list.add(row);
            }
        }
        return list;
    }
}
