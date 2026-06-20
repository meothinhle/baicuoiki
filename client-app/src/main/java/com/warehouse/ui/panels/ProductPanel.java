package com.warehouse.ui.panels;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.warehouse.service.ApiClient;
import com.warehouse.service.ClientSession;
import com.warehouse.ui.MainFrame;
import com.warehouse.util.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class ProductPanel extends JPanel implements MainFrame.RefreshablePanel {

    private DefaultTableModel model;
    private JTable table;
    private JTextField txtSearch;
    private List<JsonObject> categoryCache = new ArrayList<>();

    public ProductPanel() {
        setLayout(new BorderLayout());
        setBackground(UITheme.BG_MAIN);
        setBorder(new EmptyBorder(24, 28, 24, 28));

        // Title và toolbar đặt trên 2 dòng riêng (thay vì cùng 1 dòng
        // BorderLayout WEST/EAST) để tránh đè chồng lên nhau khi cửa sổ
        // không đủ rộng cho cả title + toolbar (nhiều nút + ô tìm kiếm).
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.add(UITheme.sectionTitleWithIcon("\uD83D\uDCE6", "Quản lý sản phẩm"), BorderLayout.WEST);
        top.add(titleRow);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolbar.setOpaque(false);
        toolbar.setBorder(new EmptyBorder(8, 0, 0, 0));
        txtSearch = UITheme.searchField("Tìm theo mã, tên, hãng...");
        JButton btnSearch = UITheme.grayButton("Tìm kiếm");
        btnSearch.addActionListener(e -> doSearch());
        txtSearch.addActionListener(e -> doSearch());
        toolbar.add(txtSearch);
        toolbar.add(btnSearch);

        JButton btnExportCsv = UITheme.grayButton("Xuất CSV");
        btnExportCsv.addActionListener(e -> exportCsv());
        toolbar.add(btnExportCsv);

        if (ClientSession.getInstance().isAdmin()) {
            JButton btnImportCsv = UITheme.grayButton("Nhập CSV");
            btnImportCsv.addActionListener(e -> importCsv());
            toolbar.add(btnImportCsv);

            JButton btnAdd = UITheme.primaryButton("+ Thêm sản phẩm");
            btnAdd.addActionListener(e -> openForm(null));
            toolbar.add(btnAdd);
        }
        top.add(toolbar);
        add(top, BorderLayout.NORTH);

        model = new DefaultTableModel(new String[]{"ID", "Mã SP", "Tên sản phẩm", "Danh mục", "Hãng", "ĐVT", "Giá nhập", "Giá bán", "Tồn kho", "Tối thiểu"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model) {
            @Override
            public Component prepareRenderer(javax.swing.table.TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                try {
                    int qty = (Integer) model.getValueAt(row, 8);
                    int minQty = (Integer) model.getValueAt(row, 9);
                    if (!isRowSelected(row)) {
                        c.setBackground(qty <= minQty ? UITheme.ROW_LOW : (row % 2 == 0 ? UITheme.ROW_EVEN : UITheme.ROW_ODD));
                    }
                } catch (Exception ignored) {}
                return c;
            }
        };
        UITheme.styleTable(table);
        // Ẩn hẳn cột ID bằng removeColumn() thay vì setMaxWidth(0): khi chỉ
        // set width = 0, JTable vẫn cố vẽ renderer/border cho cột đó, để lại
        // 1 dấu chấm/viền mảnh lỗi ở đầu mỗi dòng. removeColumn() loại bỏ
        // hoàn toàn cột khỏi TableColumnModel (UI hiển thị), trong khi dữ
        // liệu ID vẫn còn trong "model" (TableModel) để lấy qua getValueAt().
        javax.swing.table.TableColumn idColumn = table.getColumnModel().getColumn(0);
        table.removeColumn(idColumn);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER));
        add(scroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottom.setOpaque(false);
        if (ClientSession.getInstance().isAdmin()) {
            JButton btnEdit = UITheme.grayButton("Sửa");
            btnEdit.addActionListener(e -> editSelected());
            JButton btnDelete = UITheme.dangerButton("Xoá");
            btnDelete.addActionListener(e -> deleteSelected());
            bottom.add(btnEdit);
            bottom.add(btnDelete);
        }
        JPanel notePanel = new JPanel();
        notePanel.setOpaque(false);
        notePanel.setLayout(new BoxLayout(notePanel, BoxLayout.X_AXIS));
        JLabel lblNoteIcon = new JLabel("\uD83D\uDFE7");
        lblNoteIcon.setFont(UITheme.FONT_EMOJI);
        JLabel lblNoteText = new JLabel(" Màu cam = sản phẩm sắp hết hàng");
        lblNoteText.setFont(UITheme.FONT_SMALL);
        lblNoteText.setForeground(Color.GRAY);
        notePanel.add(Box.createHorizontalStrut(8));
        notePanel.add(lblNoteIcon);
        notePanel.add(lblNoteText);
        bottom.add(notePanel);
        add(bottom, BorderLayout.SOUTH);

        loadCategories();
        refreshData();
    }

    private void loadCategories() {
        SwingWorker<JsonObject, Void> worker = new SwingWorker<>() {
            @Override protected JsonObject doInBackground() { return ApiClient.getInstance().call("CATEGORY_FIND_ALL"); }
            @Override protected void done() {
                try {
                    JsonObject resp = get();
                    if (resp.get("success").getAsBoolean()) {
                        categoryCache.clear();
                        resp.getAsJsonArray("data").forEach(el -> categoryCache.add(el.getAsJsonObject()));
                    }
                } catch (Exception ignored) {}
            }
        };
        worker.execute();
    }

    @Override
    public void refreshData() {
        loadData("PRODUCT_FIND_ALL", "");
    }

    private void doSearch() {
        String kw = txtSearch.getText().trim();
        if (kw.isEmpty()) { refreshData(); return; }
        loadData("PRODUCT_SEARCH", kw);
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
                            JsonObject p = el.getAsJsonObject();
                            model.addRow(new Object[]{
                                    p.get("id").getAsInt(),
                                    p.get("code").getAsString(),
                                    p.get("name").getAsString(),
                                    str(p, "categoryName"),
                                    str(p, "brand"),
                                    str(p, "unit"),
                                    p.get("costPrice").getAsBigDecimal(),
                                    p.get("sellPrice").getAsBigDecimal(),
                                    p.get("quantity").getAsInt(),
                                    p.get("minQuantity").getAsInt()
                            });
                        }
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ProductPanel.this, "Lỗi tải dữ liệu: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private String str(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : "";
    }

    private void editSelected() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Vui lòng chọn một sản phẩm"); return; }
        int id = (Integer) model.getValueAt(row, 0);
        SwingWorker<JsonObject, Void> worker = new SwingWorker<>() {
            @Override protected JsonObject doInBackground() { return ApiClient.getInstance().call("PRODUCT_FIND_ALL"); }
            @Override protected void done() {
                try {
                    JsonObject resp = get();
                    if (resp.get("success").getAsBoolean()) {
                        for (var el : resp.getAsJsonArray("data")) {
                            JsonObject p = el.getAsJsonObject();
                            if (p.get("id").getAsInt() == id) { openForm(p); return; }
                        }
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ProductPanel.this, "Lỗi: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Vui lòng chọn một sản phẩm"); return; }
        int confirm = JOptionPane.showConfirmDialog(this, "Xoá sản phẩm \"" + model.getValueAt(row, 2) + "\"?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        int id = (Integer) model.getValueAt(row, 0);
        SwingWorker<JsonObject, Void> worker = new SwingWorker<>() {
            @Override protected JsonObject doInBackground() { return ApiClient.getInstance().call("PRODUCT_DELETE", id); }
            @Override protected void done() {
                try {
                    JsonObject resp = get();
                    JOptionPane.showMessageDialog(ProductPanel.this, resp.get("message").getAsString());
                    refreshData();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ProductPanel.this, "Lỗi: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    /**
     * Form Thêm/Sửa sản phẩm.
     * Trước đây dùng GridLayout(0,1,..) xếp label+field thành các dòng dọc
     * riêng biệt -> với ~10 field, tổng chiều cao vượt màn hình và không
     * cuộn được, không điền hết thông tin.
     * Giờ dùng GridBagLayout (label bên trái, field bên phải, cùng 1 dòng)
     * và bọc trong JScrollPane với chiều cao tối đa giới hạn, để form luôn
     * gọn và cuộn được nếu màn hình nhỏ.
     */
    private void openForm(JsonObject existing) {
        boolean isEdit = existing != null;

        if (categoryCache.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng tạo danh mục trước khi thêm sản phẩm");
            return;
        }

        JTextField txtCode = new JTextField(isEdit ? existing.get("code").getAsString() : "", 18);
        txtCode.setEnabled(!isEdit);
        JTextField txtName = new JTextField(isEdit ? existing.get("name").getAsString() : "", 18);

        JComboBox<String> cbCategory = new JComboBox<>();
        int selectedIdx = 0;
        for (int i = 0; i < categoryCache.size(); i++) {
            JsonObject c = categoryCache.get(i);
            cbCategory.addItem(c.get("name").getAsString());
            if (isEdit && c.get("id").getAsInt() == existing.get("categoryId").getAsInt()) selectedIdx = i;
        }
        cbCategory.setSelectedIndex(selectedIdx);

        JTextField txtBrand = new JTextField(isEdit ? str(existing, "brand") : "", 18);
        JTextField txtUnit = new JTextField(isEdit ? str(existing, "unit") : "Cái", 18);
        JTextField txtCostPrice = new JTextField(isEdit ? existing.get("costPrice").getAsBigDecimal().toPlainString() : "0", 18);
        JTextField txtSellPrice = new JTextField(isEdit ? existing.get("sellPrice").getAsBigDecimal().toPlainString() : "0", 18);
        JTextField txtQuantity = new JTextField(isEdit ? String.valueOf(existing.get("quantity").getAsInt()) : "0", 18);
        txtQuantity.setEnabled(false);
        JTextField txtMinQty = new JTextField(isEdit ? String.valueOf(existing.get("minQuantity").getAsInt()) : "5", 18);
        JTextArea txtDesc = new JTextArea(isEdit ? str(existing, "description") : "", 3, 18);
        txtDesc.setLineWrap(true);
        txtDesc.setWrapStyleWord(true);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;
        row = addFormRow(form, gbc, row, "Mã sản phẩm:", txtCode);
        row = addFormRow(form, gbc, row, "Tên sản phẩm:", txtName);
        row = addFormRow(form, gbc, row, "Danh mục:", cbCategory);
        row = addFormRow(form, gbc, row, "Hãng sản xuất:", txtBrand);
        row = addFormRow(form, gbc, row, "Đơn vị tính:", txtUnit);
        row = addFormRow(form, gbc, row, "Giá nhập (VNĐ):", txtCostPrice);
        row = addFormRow(form, gbc, row, "Giá bán (VNĐ):", txtSellPrice);
        if (isEdit) {
            row = addFormRow(form, gbc, row, "Tồn kho (chỉ xem):", txtQuantity);
        }
        row = addFormRow(form, gbc, row, "Mức tồn tối thiểu:", txtMinQty);

        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE;
        form.add(new JLabel("Mô tả:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.BOTH; gbc.weightx = 1;
        JScrollPane descScroll = new JScrollPane(txtDesc);
        descScroll.setPreferredSize(new Dimension(220, 60));
        form.add(descScroll, gbc);

        // Bọc form trong JScrollPane với chiều cao giới hạn,
        // đảm bảo luôn vừa màn hình và cuộn được khi cần.
        JScrollPane formScroll = new JScrollPane(form);
        formScroll.setBorder(BorderFactory.createEmptyBorder());
        formScroll.getVerticalScrollBar().setUnitIncrement(16);
        formScroll.setPreferredSize(new Dimension(420, Math.min(form.getPreferredSize().height + 20, 480)));

        int result = JOptionPane.showConfirmDialog(this, formScroll, isEdit ? "Sửa sản phẩm" : "Thêm sản phẩm",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        if (txtCode.getText().isBlank() || txtName.getText().isBlank()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ mã và tên sản phẩm");
            return;
        }

        BigDecimal costPrice, sellPrice;
        int minQty;
        try {
            costPrice = new BigDecimal(txtCostPrice.getText().trim());
            sellPrice = new BigDecimal(txtSellPrice.getText().trim());
            minQty = Integer.parseInt(txtMinQty.getText().trim());
            if (sellPrice.compareTo(BigDecimal.ZERO) <= 0) {
                JOptionPane.showMessageDialog(this, "Giá bán phải lớn hơn 0");
                return;
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Giá hoặc số lượng không hợp lệ");
            return;
        }

        int categoryId = categoryCache.get(cbCategory.getSelectedIndex()).get("id").getAsInt();

        JsonObject data = new JsonObject();
        if (isEdit) {
            data.addProperty("id", existing.get("id").getAsInt());
            data.addProperty("quantity", existing.get("quantity").getAsInt());
        } else {
            data.addProperty("quantity", 0);
        }
        data.addProperty("code", txtCode.getText().trim());
        data.addProperty("name", txtName.getText().trim());
        data.addProperty("categoryId", categoryId);
        data.addProperty("brand", txtBrand.getText().trim());
        data.addProperty("unit", txtUnit.getText().trim().isEmpty() ? "Cái" : txtUnit.getText().trim());
        data.addProperty("costPrice", costPrice);
        data.addProperty("sellPrice", sellPrice);
        data.addProperty("minQuantity", minQty);
        data.addProperty("description", txtDesc.getText().trim());
        data.addProperty("status", "ACTIVE");

        String action = isEdit ? "PRODUCT_UPDATE" : "PRODUCT_CREATE";
        SwingWorker<JsonObject, Void> worker = new SwingWorker<>() {
            @Override protected JsonObject doInBackground() { return ApiClient.getInstance().call(action, data); }
            @Override protected void done() {
                try {
                    JsonObject resp = get();
                    JOptionPane.showMessageDialog(ProductPanel.this, resp.get("message").getAsString());
                    if (resp.get("success").getAsBoolean()) refreshData();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ProductPanel.this, "Lỗi: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    /** Thêm 1 dòng "label: field" vào form dùng GridBagLayout, trả về dòng kế tiếp */
    private int addFormRow(JPanel form, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridx = 0; gbc.gridy = row;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        form.add(new JLabel(label), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        form.add(field, gbc);
        return row + 1;
    }

    private void exportCsv() {
        SwingWorker<JsonObject, Void> worker = new SwingWorker<>() {
            @Override protected JsonObject doInBackground() { return ApiClient.getInstance().call("FILE_EXPORT_PRODUCTS_CSV"); }
            @Override protected void done() {
                try {
                    JsonObject resp = get();
                    if (!resp.get("success").getAsBoolean()) {
                        JOptionPane.showMessageDialog(ProductPanel.this, resp.get("message").getAsString());
                        return;
                    }
                    String csv = resp.get("data").getAsString();
                    JFileChooser chooser = new JFileChooser();
                    chooser.setSelectedFile(new java.io.File("products_export.csv"));
                    if (chooser.showSaveDialog(ProductPanel.this) == JFileChooser.APPROVE_OPTION) {
                        Files.writeString(chooser.getSelectedFile().toPath(), csv, StandardCharsets.UTF_8);
                        JOptionPane.showMessageDialog(ProductPanel.this, "Đã lưu file: " + chooser.getSelectedFile().getName());
                    }
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(ProductPanel.this, "Lỗi ghi file: " + ex.getMessage());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ProductPanel.this, "Lỗi: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void importCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV files", "csv"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            String content = Files.readString(chooser.getSelectedFile().toPath(), StandardCharsets.UTF_8);
            SwingWorker<JsonObject, Void> worker = new SwingWorker<>() {
                @Override protected JsonObject doInBackground() { return ApiClient.getInstance().call("FILE_IMPORT_PRODUCTS_CSV", content); }
                @Override protected void done() {
                    try {
                        JsonObject resp = get();
                        JOptionPane.showMessageDialog(ProductPanel.this, resp.get("message").getAsString());
                        if (resp.get("success").getAsBoolean()) { refreshData(); loadCategories(); }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(ProductPanel.this, "Lỗi: " + ex.getMessage());
                    }
                }
            };
            worker.execute();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Lỗi đọc file: " + ex.getMessage());
        }
    }
}