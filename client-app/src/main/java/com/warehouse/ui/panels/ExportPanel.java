package com.warehouse.ui.panels;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.warehouse.service.ApiClient;
import com.warehouse.ui.MainFrame;
import com.warehouse.util.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ExportPanel extends JPanel implements MainFrame.RefreshablePanel {

    private DefaultTableModel model;
    private JTable table;
    private JTextField txtSearch;

    public ExportPanel() {
        setLayout(new BorderLayout());
        setBackground(UITheme.BG_MAIN);
        setBorder(new EmptyBorder(24, 28, 24, 28));

        // Title và toolbar đặt trên 2 dòng riêng, tránh đè chồng khi cửa sổ hẹp.
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.add(UITheme.sectionTitleWithIcon("\u2B06", "Phiếu xuất kho"), BorderLayout.WEST);
        top.add(titleRow);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolbar.setOpaque(false);
        toolbar.setBorder(new EmptyBorder(8, 0, 0, 0));
        txtSearch = UITheme.searchField("Tìm theo mã phiếu, khách hàng...");
        JButton btnSearch = UITheme.grayButton("Tìm kiếm");
        btnSearch.addActionListener(e -> doSearch());
        txtSearch.addActionListener(e -> doSearch());
        toolbar.add(txtSearch);
        toolbar.add(btnSearch);

        JButton btnAdd = UITheme.primaryButton("+ Tạo phiếu xuất");
        btnAdd.addActionListener(e -> openCreateDialog());
        toolbar.add(btnAdd);
        top.add(toolbar);
        add(top, BorderLayout.NORTH);

        model = new DefaultTableModel(new String[]{"ID", "Mã phiếu", "Khách hàng", "SĐT", "Người tạo", "Tổng tiền", "Ngày xuất"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        UITheme.styleTable(table);
        // removeColumn() loại bỏ hẳn cột khỏi UI, tránh dấu chấm/viền lỗi
        // còn sót lại khi chỉ set width = 0 (xem ProductPanel.java).
        table.removeColumn(table.getColumnModel().getColumn(0));

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER));
        add(scroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottom.setOpaque(false);
        JButton btnDetail = UITheme.grayButton("Xem chi tiết");
        btnDetail.addActionListener(e -> viewDetail());
        bottom.add(btnDetail);
        add(bottom, BorderLayout.SOUTH);

        refreshData();
    }

    @Override
    public void refreshData() {
        loadData("EXPORT_FIND_ALL", "");
    }

    private void doSearch() {
        String kw = txtSearch.getText().trim();
        if (kw.isEmpty()) { refreshData(); return; }
        loadData("EXPORT_SEARCH", kw);
    }

    private void loadData(String action, String keyword) {
        SwingWorker<JsonObject, Void> worker = new SwingWorker<>() {
            @Override protected JsonObject doInBackground() {
                return keyword.isEmpty() ? ApiClient.getInstance().call(action) : ApiClient.getInstance().call(action, keyword);
            }
            @Override protected void done() {
                try {
                    JsonObject resp = get();
                    if (resp.has("success") && resp.get("success").getAsBoolean()) {
                        model.setRowCount(0);
                        JsonArray arr = resp.getAsJsonArray("data");
                        for (var el : arr) {
                            JsonObject o = el.getAsJsonObject();
                            model.addRow(new Object[]{
                                    o.get("id").getAsInt(),
                                    o.get("code").getAsString(),
                                    str(o, "customerName"),
                                    str(o, "customerPhone"),
                                    str(o, "userName"),
                                    o.get("totalAmount").getAsBigDecimal(),
                                    str(o, "exportDate")
                            });
                        }
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ExportPanel.this, "Lỗi tải dữ liệu: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private String str(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : "";
    }

    private void viewDetail() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Vui lòng chọn một phiếu xuất"); return; }
        int id = (Integer) model.getValueAt(row, 0);
        SwingWorker<JsonObject, Void> worker = new SwingWorker<>() {
            @Override protected JsonObject doInBackground() { return ApiClient.getInstance().call("EXPORT_FIND_BY_ID", id); }
            @Override protected void done() {
                try {
                    JsonObject resp = get();
                    if (!resp.get("success").getAsBoolean()) { JOptionPane.showMessageDialog(ExportPanel.this, resp.get("message").getAsString()); return; }
                    showDetailDialog(resp.getAsJsonObject("data"));
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ExportPanel.this, "Lỗi: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void showDetailDialog(JsonObject order) {
        DefaultTableModel detailModel = new DefaultTableModel(new String[]{"Mã SP", "Tên sản phẩm", "ĐVT", "Số lượng", "Đơn giá", "Thành tiền"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (var el : order.getAsJsonArray("items")) {
            JsonObject item = el.getAsJsonObject();
            detailModel.addRow(new Object[]{
                    item.get("productCode").getAsString(),
                    item.get("productName").getAsString(),
                    item.get("productUnit").getAsString(),
                    item.get("quantity").getAsInt(),
                    item.get("unitPrice").getAsBigDecimal(),
                    item.get("totalPrice").getAsBigDecimal()
            });
        }
        JTable detailTable = new JTable(detailModel);
        UITheme.styleTable(detailTable);
        JScrollPane scroll = new JScrollPane(detailTable);
        scroll.setPreferredSize(new Dimension(700, 300));

        JPanel panel = new JPanel(new BorderLayout(0, 8));
        JLabel lblInfo = new JLabel("<html>Mã phiếu: <b>" + order.get("code").getAsString() + "</b> &nbsp;|&nbsp; KH: <b>" +
                str(order, "customerName") + "</b> &nbsp;|&nbsp; Tổng tiền: <b>" + order.get("totalAmount").getAsBigDecimal() + " đ</b></html>");
        panel.add(lblInfo, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);

        JOptionPane.showMessageDialog(this, panel, "Chi tiết phiếu xuất kho", JOptionPane.PLAIN_MESSAGE);
    }

    private void openCreateDialog() {
        new CreateExportDialog((Frame) SwingUtilities.getWindowAncestor(this), this).setVisible(true);
    }

    static class CreateExportDialog extends JDialog {
        private final ExportPanel parent;
        private List<JsonObject> productCache = new ArrayList<>();
        private JComboBox<String> cbProduct;
        private JTextField txtQty, txtPrice, txtCustomerName, txtCustomerPhone;
        private DefaultTableModel itemsModel;
        private JTable itemsTable;
        private JLabel lblTotal, lblStock;
        private JTextArea txtNote;
        private List<JsonObject> currentItems = new ArrayList<>();

        public CreateExportDialog(Frame owner, ExportPanel parent) {
            super(owner, "Tạo phiếu xuất kho", true);
            this.parent = parent;
            setSize(800, 620);
            setLocationRelativeTo(owner);
            buildUI();
            loadProducts();
        }

        private void buildUI() {
            setLayout(new BorderLayout(10, 10));
            JPanel root = new JPanel(new BorderLayout(10, 10));
            root.setBorder(new EmptyBorder(15, 15, 15, 15));

            JPanel header = new JPanel(new GridLayout(2, 1, 4, 4));
            JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
            row1.add(new JLabel("Tên khách hàng:"));
            txtCustomerName = new JTextField(20);
            row1.add(txtCustomerName);
            row1.add(new JLabel("SĐT:"));
            txtCustomerPhone = new JTextField(15);
            row1.add(txtCustomerPhone);
            header.add(row1);

            JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
            row2.add(new JLabel("Ghi chú:"));
            txtNote = new JTextArea(1, 30);
            row2.add(new JScrollPane(txtNote));
            header.add(row2);
            root.add(header, BorderLayout.NORTH);

            JPanel center = new JPanel(new BorderLayout(8, 8));
            JPanel addRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
            addRow.add(new JLabel("Sản phẩm:"));
            cbProduct = new JComboBox<>();
            cbProduct.setPreferredSize(new Dimension(280, 28));
            cbProduct.addActionListener(e -> updateSuggestedPrice());
            addRow.add(cbProduct);
            addRow.add(new JLabel("SL:"));
            txtQty = new JTextField("1", 5);
            addRow.add(txtQty);
            addRow.add(new JLabel("Đơn giá:"));
            txtPrice = new JTextField(10);
            addRow.add(txtPrice);
            JButton btnAddItem = UITheme.primaryButton("Thêm vào phiếu");
            btnAddItem.addActionListener(e -> addItemToTable());
            addRow.add(btnAddItem);
            center.add(addRow, BorderLayout.NORTH);

            lblStock = new JLabel(" ");
            lblStock.setFont(UITheme.FONT_SMALL);
            lblStock.setForeground(UITheme.SUCCESS);

            itemsModel = new DefaultTableModel(new String[]{"Mã SP", "Tên sản phẩm", "ĐVT", "Số lượng", "Đơn giá", "Thành tiền"}, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };
            itemsTable = new JTable(itemsModel);
            UITheme.styleTable(itemsTable);
            JScrollPane scroll = new JScrollPane(itemsTable);

            JPanel centerWrap = new JPanel(new BorderLayout());
            centerWrap.add(lblStock, BorderLayout.NORTH);
            centerWrap.add(scroll, BorderLayout.CENTER);
            center.add(centerWrap, BorderLayout.CENTER);

            JPanel south = new JPanel(new BorderLayout());
            lblTotal = new JLabel("Tổng tiền: 0 đ");
            lblTotal.setFont(UITheme.FONT_BOLD);
            JButton btnRemove = UITheme.dangerButton("Xoá dòng đã chọn");
            btnRemove.addActionListener(e -> removeSelectedItem());
            JPanel southLeft = new JPanel(new FlowLayout(FlowLayout.LEFT));
            southLeft.add(btnRemove);
            south.add(southLeft, BorderLayout.WEST);
            south.add(lblTotal, BorderLayout.EAST);
            center.add(south, BorderLayout.SOUTH);

            root.add(center, BorderLayout.CENTER);

            JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton btnSave = UITheme.successButton("Lưu phiếu xuất");
            btnSave.addActionListener(e -> saveOrder());
            JButton btnCancel = UITheme.grayButton("Huỷ");
            btnCancel.addActionListener(e -> dispose());
            footer.add(btnCancel);
            footer.add(btnSave);
            root.add(footer, BorderLayout.SOUTH);

            add(root);
        }

        private void loadProducts() {
            SwingWorker<JsonObject, Void> worker = new SwingWorker<>() {
                @Override protected JsonObject doInBackground() { return ApiClient.getInstance().call("PRODUCT_FIND_ALL"); }
                @Override protected void done() {
                    try {
                        JsonObject resp = get();
                        if (resp.get("success").getAsBoolean()) {
                            productCache.clear();
                            cbProduct.removeAllItems();
                            for (var el : resp.getAsJsonArray("data")) {
                                JsonObject p = el.getAsJsonObject();
                                productCache.add(p);
                                cbProduct.addItem(p.get("code").getAsString() + " - " + p.get("name").getAsString());
                            }
                            updateSuggestedPrice();
                        }
                    } catch (Exception ignored) {}
                }
            };
            worker.execute();
        }

        private void updateSuggestedPrice() {
            int idx = cbProduct.getSelectedIndex();
            if (idx >= 0 && idx < productCache.size()) {
                JsonObject p = productCache.get(idx);
                txtPrice.setText(p.get("sellPrice").getAsBigDecimal().toPlainString());
                lblStock.setText("Tồn kho hiện có: " + p.get("quantity").getAsInt() + " " + p.get("unit").getAsString());
            }
        }

        private void addItemToTable() {
            int idx = cbProduct.getSelectedIndex();
            if (idx < 0) { JOptionPane.showMessageDialog(this, "Vui lòng chọn sản phẩm"); return; }
            int qty;
            BigDecimal price;
            try {
                qty = Integer.parseInt(txtQty.getText().trim());
                price = new BigDecimal(txtPrice.getText().trim());
                if (qty <= 0) { JOptionPane.showMessageDialog(this, "Số lượng phải lớn hơn 0"); return; }
                if (price.compareTo(BigDecimal.ZERO) < 0) { JOptionPane.showMessageDialog(this, "Đơn giá không hợp lệ"); return; }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Số lượng hoặc đơn giá không hợp lệ");
                return;
            }
            JsonObject p = productCache.get(idx);
            int available = p.get("quantity").getAsInt();
            int alreadyAdded = 0;
            for (JsonObject it : currentItems) {
                if (it.get("productId").getAsInt() == p.get("id").getAsInt()) alreadyAdded += it.get("quantity").getAsInt();
            }
            if (qty + alreadyAdded > available) {
                JOptionPane.showMessageDialog(this, "Tồn kho không đủ! Hiện có: " + available + ", đã thêm: " + alreadyAdded);
                return;
            }

            JsonObject item = new JsonObject();
            item.addProperty("productId", p.get("id").getAsInt());
            item.addProperty("productCode", p.get("code").getAsString());
            item.addProperty("productName", p.get("name").getAsString());
            item.addProperty("productUnit", p.get("unit").getAsString());
            item.addProperty("quantity", qty);
            item.addProperty("unitPrice", price);
            currentItems.add(item);

            BigDecimal total = price.multiply(BigDecimal.valueOf(qty));
            itemsModel.addRow(new Object[]{
                    p.get("code").getAsString(), p.get("name").getAsString(), p.get("unit").getAsString(), qty, price, total
            });
            updateTotal();
        }

        private void removeSelectedItem() {
            int row = itemsTable.getSelectedRow();
            if (row < 0) return;
            currentItems.remove(row);
            itemsModel.removeRow(row);
            updateTotal();
        }

        private void updateTotal() {
            BigDecimal total = BigDecimal.ZERO;
            for (int i = 0; i < itemsModel.getRowCount(); i++) {
                total = total.add((BigDecimal) itemsModel.getValueAt(i, 5));
            }
            lblTotal.setText("Tổng tiền: " + total + " đ");
        }

        private void saveOrder() {
            if (currentItems.isEmpty()) { JOptionPane.showMessageDialog(this, "Vui lòng thêm ít nhất 1 sản phẩm"); return; }
            if (txtCustomerName.getText().isBlank()) { JOptionPane.showMessageDialog(this, "Vui lòng nhập tên khách hàng"); return; }

            BigDecimal total = BigDecimal.ZERO;
            for (JsonObject item : currentItems) {
                BigDecimal qty = BigDecimal.valueOf(item.get("quantity").getAsInt());
                total = total.add(item.get("unitPrice").getAsBigDecimal().multiply(qty));
            }
            final BigDecimal finalTotal = total;

            SwingWorker<JsonObject, Void> codeWorker = new SwingWorker<>() {
                @Override protected JsonObject doInBackground() { return ApiClient.getInstance().call("EXPORT_GENERATE_CODE"); }
                @Override protected void done() {
                    try {
                        JsonObject codeResp = get();
                        String code = codeResp.get("data").getAsString();

                        JsonObject order = new JsonObject();
                        order.addProperty("code", code);
                        order.addProperty("customerName", txtCustomerName.getText().trim());
                        order.addProperty("customerPhone", txtCustomerPhone.getText().trim());
                        order.addProperty("totalAmount", finalTotal);
                        order.addProperty("note", txtNote.getText().trim());
                        JsonArray itemsArr = new JsonArray();
                        currentItems.forEach(itemsArr::add);
                        order.add("items", itemsArr);

                        SwingWorker<JsonObject, Void> saveWorker = new SwingWorker<>() {
                            @Override protected JsonObject doInBackground() { return ApiClient.getInstance().call("EXPORT_CREATE", order); }
                            @Override protected void done() {
                                try {
                                    JsonObject resp = get();
                                    JOptionPane.showMessageDialog(CreateExportDialog.this, resp.get("message").getAsString());
                                    if (resp.get("success").getAsBoolean()) {
                                        parent.refreshData();
                                        dispose();
                                    }
                                } catch (Exception ex) {
                                    JOptionPane.showMessageDialog(CreateExportDialog.this, "Lỗi: " + ex.getMessage());
                                }
                            }
                        };
                        saveWorker.execute();
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(CreateExportDialog.this, "Lỗi: " + ex.getMessage());
                    }
                }
            };
            codeWorker.execute();
        }
    }
}