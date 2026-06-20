package com.warehouse.dao;

import com.warehouse.model.ImportOrder;
import com.warehouse.model.ImportOrderItem;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class ImportOrderDAO {
    private static final Logger logger = Logger.getLogger(ImportOrderDAO.class.getName());

    private ProductDAO productDAO = new ProductDAO();

    public List<ImportOrder> findAll() {
        List<ImportOrder> list = new ArrayList<>();
        String sql = "SELECT io.*, s.name as supplier_name, u.full_name as user_name " +
                "FROM import_orders io LEFT JOIN suppliers s ON io.supplier_id=s.id " +
                "LEFT JOIN users u ON io.user_id=u.id ORDER BY io.import_date DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            logger.severe("Lỗi findAll importOrders: " + e.getMessage());
        }
        return list;
    }

    public ImportOrder findById(int id) {
        String sql = "SELECT io.*, s.name as supplier_name, u.full_name as user_name " +
                "FROM import_orders io LEFT JOIN suppliers s ON io.supplier_id=s.id " +
                "LEFT JOIN users u ON io.user_id=u.id WHERE io.id=?";
        ImportOrder order = null;
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
            logger.severe("Lỗi findById importOrder: " + e.getMessage());
        }
        return order;
    }

    /**
     * Tạo phiếu nhập kho - dùng Transaction:
     * 1. Thêm phiếu nhập vào import_orders
     * 2. Thêm từng chi tiết vào import_order_items
     * 3. Cập nhật số lượng tồn kho trong products
     * Nếu bất kỳ bước nào lỗi -> rollback toàn bộ
     */
    public boolean insertWithTransaction(ImportOrder order) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);  // Bắt đầu transaction

            // Tự tính lại tổng tiền phiếu từ các item (đơn giá * số lượng),
            // không phụ thuộc vào order.getTotalAmount() đã được client tính
            // sẵn hay chưa - tránh lệch dữ liệu hoặc vi phạm NOT NULL.
            BigDecimal totalAmount = BigDecimal.ZERO;
            for (ImportOrderItem item : order.getItems()) {
                BigDecimal unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
                totalAmount = totalAmount.add(unitPrice.multiply(BigDecimal.valueOf(item.getQuantity())));
            }
            order.setTotalAmount(totalAmount);

            // Bước 1: Thêm phiếu nhập
            int orderId = insertOrder(conn, order);
            if (orderId <= 0) throw new SQLException("Không tạo được phiếu nhập");

            // Bước 2 & 3: Thêm chi tiết và cập nhật tồn kho
            for (ImportOrderItem item : order.getItems()) {
                item.setImportOrderId(orderId);
                if (!insertItem(conn, item)) throw new SQLException("Không thêm được chi tiết phiếu nhập: " + item.getProductCode());
                // Cập nhật số lượng: nhập kho -> tăng quantity
                if (!productDAO.updateQuantity(conn, item.getProductId(), item.getQuantity())) {
                    throw new SQLException("Không cập nhật được tồn kho: " + item.getProductCode());
                }
            }

            conn.commit();  // Commit transaction
            return true;
        } catch (SQLException e) {
            logger.severe("Lỗi insertWithTransaction importOrder: " + e.getMessage()
                    + " | SQLState=" + e.getSQLState() + " | ErrorCode=" + e.getErrorCode());
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { logger.severe("Lỗi rollback: " + ex.getMessage()); }
            }
            return false;
        } finally {
            DatabaseConnection.closeConnection(conn);
        }
    }

    private int insertOrder(Connection conn, ImportOrder order) throws SQLException {
        String sql = "INSERT INTO import_orders (code, supplier_id, user_id, total_amount, note, status, import_date) VALUES (?,?,?,?,?,?,NOW())";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, order.getCode());
            ps.setInt(2, order.getSupplierId());
            ps.setInt(3, order.getUserId());
            ps.setBigDecimal(4, order.getTotalAmount());
            ps.setString(5, order.getNote());
            ps.setString(6, "COMPLETED");
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        return -1;
    }

    private boolean insertItem(Connection conn, ImportOrderItem item) throws SQLException {
        String sql = "INSERT INTO import_order_items (import_order_id, product_id, quantity, unit_price, total_price) VALUES (?,?,?,?,?)";
        // Luôn tự tính total_price = unit_price * quantity ngay tại đây,
        // không phụ thuộc vào item.getTotalPrice() đã được set sẵn từ phía
        // gọi hàm (client/service) hay chưa. Nếu để null/thiếu, cột
        // total_price (NOT NULL trong DB) sẽ bị vi phạm và insert thất bại,
        // làm rollback toàn bộ phiếu nhập ("Tạo phiếu nhập thất bại").
        BigDecimal unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
        BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
        item.setTotalPrice(totalPrice);

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, item.getImportOrderId());
            ps.setInt(2, item.getProductId());
            ps.setInt(3, item.getQuantity());
            ps.setBigDecimal(4, unitPrice);
            ps.setBigDecimal(5, totalPrice);
            return ps.executeUpdate() > 0;
        }
    }

    private List<ImportOrderItem> findItems(Connection conn, int orderId) throws SQLException {
        List<ImportOrderItem> items = new ArrayList<>();
        String sql = "SELECT ioi.*, p.code as product_code, p.name as product_name, p.unit as product_unit " +
                "FROM import_order_items ioi LEFT JOIN products p ON ioi.product_id=p.id WHERE ioi.import_order_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ImportOrderItem item = new ImportOrderItem();
                    item.setId(rs.getInt("id"));
                    item.setImportOrderId(orderId);
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

    public List<ImportOrder> search(String keyword) {
        List<ImportOrder> list = new ArrayList<>();
        String sql = "SELECT io.*, s.name as supplier_name, u.full_name as user_name " +
                "FROM import_orders io LEFT JOIN suppliers s ON io.supplier_id=s.id " +
                "LEFT JOIN users u ON io.user_id=u.id WHERE io.code LIKE ? OR s.name LIKE ? ORDER BY io.import_date DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String kw = "%" + keyword + "%";
            ps.setString(1, kw); ps.setString(2, kw);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.severe("Lỗi search importOrder: " + e.getMessage());
        }
        return list;
    }

    public String generateCode() {
        String sql = "SELECT COUNT(*)+1 as next_num FROM import_orders WHERE DATE(created_at) = CURDATE()";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return String.format("NK%s%03d", java.time.LocalDate.now().toString().replace("-", ""), rs.getInt("next_num"));
            }
        } catch (SQLException e) {
            logger.severe("Lỗi generateCode: " + e.getMessage());
        }
        return "NK" + System.currentTimeMillis();
    }

    private ImportOrder mapRow(ResultSet rs) throws SQLException {
        ImportOrder o = new ImportOrder();
        o.setId(rs.getInt("id"));
        o.setCode(rs.getString("code"));
        o.setSupplierId(rs.getInt("supplier_id"));
        o.setSupplierName(rs.getString("supplier_name"));
        o.setUserId(rs.getInt("user_id"));
        o.setUserName(rs.getString("user_name"));
        o.setTotalAmount(rs.getBigDecimal("total_amount"));
        o.setNote(rs.getString("note"));
        o.setStatus(rs.getString("status"));
        Timestamp id = rs.getTimestamp("import_date");
        if (id != null) o.setImportDate(id.toLocalDateTime());
        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null) o.setCreatedAt(ca.toLocalDateTime());
        return o;
    }
}