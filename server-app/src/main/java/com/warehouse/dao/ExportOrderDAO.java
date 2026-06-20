package com.warehouse.dao;

import com.warehouse.model.ExportOrder;
import com.warehouse.model.ExportOrderItem;
import com.warehouse.security.AESUtil;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class ExportOrderDAO {
    private static final Logger logger = Logger.getLogger(ExportOrderDAO.class.getName());
    private ProductDAO productDAO = new ProductDAO();

    public List<ExportOrder> findAll() {
        List<ExportOrder> list = new ArrayList<>();
        String sql = "SELECT eo.*, u.full_name as user_name FROM export_orders eo " +
                "LEFT JOIN users u ON eo.user_id=u.id ORDER BY eo.export_date DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            logger.severe("Lỗi findAll exportOrders: " + e.getMessage());
        }
        return list;
    }

    public ExportOrder findById(int id) {
        String sql = "SELECT eo.*, u.full_name as user_name FROM export_orders eo " +
                "LEFT JOIN users u ON eo.user_id=u.id WHERE eo.id=?";
        ExportOrder order = null;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    order = mapRow(rs);
                    order.setItems(findItems(conn, id));
                }
            }
        } catch (SQLException e) {
            logger.severe("Lỗi findById exportOrder: " + e.getMessage());
        }
        return order;
    }

    /**
     * Tạo phiếu xuất kho - Transaction:
     * 1. Kiểm tra tồn kho đủ không
     * 2. Thêm phiếu xuất
     * 3. Thêm chi tiết
     * 4. Trừ tồn kho
     */
    public boolean insertWithTransaction(ExportOrder order) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // Tự tính lại tổng tiền phiếu từ các item (đơn giá * số lượng),
            // không phụ thuộc vào order.getTotalAmount() đã được client tính
            // sẵn hay chưa - tránh lệch dữ liệu hoặc vi phạm NOT NULL.
            BigDecimal totalAmount = BigDecimal.ZERO;
            for (ExportOrderItem item : order.getItems()) {
                BigDecimal unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
                totalAmount = totalAmount.add(unitPrice.multiply(BigDecimal.valueOf(item.getQuantity())));
            }
            order.setTotalAmount(totalAmount);

            // Bước 1: Thêm phiếu xuất
            int orderId = insertOrder(conn, order);
            if (orderId <= 0) throw new SQLException("Không tạo được phiếu xuất");

            // Bước 2 & 3: Thêm chi tiết và trừ tồn kho
            for (ExportOrderItem item : order.getItems()) {
                item.setExportOrderId(orderId);
                if (!insertItem(conn, item)) throw new SQLException("Không thêm được chi tiết: " + item.getProductCode());
                // Xuất kho -> giảm quantity (delta âm)
                if (!productDAO.updateQuantity(conn, item.getProductId(), -item.getQuantity())) {
                    throw new SQLException("Không cập nhật được tồn kho: " + item.getProductCode());
                }
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            logger.severe("Lỗi insertWithTransaction exportOrder: " + e.getMessage()
                    + " | SQLState=" + e.getSQLState() + " | ErrorCode=" + e.getErrorCode());
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { logger.severe("Lỗi rollback: " + ex.getMessage()); }
            }
            return false;
        } finally {
            DatabaseConnection.closeConnection(conn);
        }
    }

    private int insertOrder(Connection conn, ExportOrder order) throws SQLException {
        String sql = "INSERT INTO export_orders (code, customer_name, customer_phone, user_id, total_amount, note, status, export_date) VALUES (?,?,?,?,?,?,?,NOW())";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, order.getCode());
            ps.setString(2, order.getCustomerName());
            String encPhone = order.getCustomerPhone() != null ? AESUtil.encrypt(order.getCustomerPhone()) : null;
            ps.setString(3, encPhone);
            ps.setInt(4, order.getUserId());
            ps.setBigDecimal(5, order.getTotalAmount());
            ps.setString(6, order.getNote());
            ps.setString(7, "COMPLETED");
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        return -1;
    }

    private boolean insertItem(Connection conn, ExportOrderItem item) throws SQLException {
        String sql = "INSERT INTO export_order_items (export_order_id, product_id, quantity, unit_price, total_price) VALUES (?,?,?,?,?)";
        // Luôn tự tính total_price = unit_price * quantity ngay tại đây,
        // không phụ thuộc vào item.getTotalPrice() đã được set sẵn hay chưa
        // (xem ImportOrderDAO.insertItem() để biết lý do/nguyên nhân lỗi gốc).
        BigDecimal unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
        BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
        item.setTotalPrice(totalPrice);

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, item.getExportOrderId());
            ps.setInt(2, item.getProductId());
            ps.setInt(3, item.getQuantity());
            ps.setBigDecimal(4, unitPrice);
            ps.setBigDecimal(5, totalPrice);
            return ps.executeUpdate() > 0;
        }
    }

    private List<ExportOrderItem> findItems(Connection conn, int orderId) throws SQLException {
        List<ExportOrderItem> items = new ArrayList<>();
        String sql = "SELECT eoi.*, p.code as product_code, p.name as product_name, p.unit as product_unit " +
                "FROM export_order_items eoi LEFT JOIN products p ON eoi.product_id=p.id WHERE eoi.export_order_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ExportOrderItem item = new ExportOrderItem();
                    item.setId(rs.getInt("id"));
                    item.setExportOrderId(orderId);
                    item.setProductId(rs.getInt("product_id"));
                    item.setProductCode(rs.getString("product_code"));
                    item.setProductName(rs.getString("product_name"));
                    item.setProductUnit(rs.getString("product_unit"));
                    item.setQuantity(rs.getInt("quantity"));
                    item.setUnitPrice(rs.getBigDecimal("unit_price"));
                    item.setTotalPrice(rs.getBigDecimal("total_price"));
                    items.add(item);
                }
            }
        }
        return items;
    }

    public List<ExportOrder> search(String keyword) {
        List<ExportOrder> list = new ArrayList<>();
        String sql = "SELECT eo.*, u.full_name as user_name FROM export_orders eo " +
                "LEFT JOIN users u ON eo.user_id=u.id WHERE eo.code LIKE ? OR eo.customer_name LIKE ? ORDER BY eo.export_date DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String kw = "%" + keyword + "%";
            ps.setString(1, kw); ps.setString(2, kw);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.severe("Lỗi search exportOrder: " + e.getMessage());
        }
        return list;
    }

    public String generateCode() {
        String sql = "SELECT COUNT(*)+1 as next_num FROM export_orders WHERE DATE(created_at) = CURDATE()";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return String.format("XK%s%03d", java.time.LocalDate.now().toString().replace("-", ""), rs.getInt("next_num"));
            }
        } catch (SQLException e) {
            logger.severe("Lỗi generateCode: " + e.getMessage());
        }
        return "XK" + System.currentTimeMillis();
    }

    private ExportOrder mapRow(ResultSet rs) throws SQLException {
        ExportOrder o = new ExportOrder();
        o.setId(rs.getInt("id"));
        o.setCode(rs.getString("code"));
        o.setCustomerName(rs.getString("customer_name"));
        String encPhone = rs.getString("customer_phone");
        o.setCustomerPhone(encPhone != null ? AESUtil.decrypt(encPhone) : null);
        o.setUserId(rs.getInt("user_id"));
        o.setUserName(rs.getString("user_name"));
        o.setTotalAmount(rs.getBigDecimal("total_amount"));
        o.setNote(rs.getString("note"));
        o.setStatus(rs.getString("status"));
        Timestamp ed = rs.getTimestamp("export_date");
        if (ed != null) o.setExportDate(ed.toLocalDateTime());
        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null) o.setCreatedAt(ca.toLocalDateTime());
        return o;
    }
}