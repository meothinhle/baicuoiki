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

public class CategoryPanel extends JPanel implements MainFrame.RefreshablePanel {

    private DefaultTableModel model;
    private JTable table;
    private JTextField txtSearch;

    public CategoryPanel() {
        setLayout(new BorderLayout());
        setBackground(UITheme.BG_MAIN);
        setBorder(new EmptyBorder(24, 28, 24, 28));

        // Title và toolbar đặt trên 2 dòng riêng, tránh đè chồng khi cửa sổ hẹp.
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.add(UITheme.sectionTitleWithIcon("\uD83D\uDDC2\uFE0F", "Quản lý danh mục"), BorderLayout.WEST);
        top.add(titleRow);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolbar.setOpaque(false);
        toolbar.setBorder(new EmptyBorder(8, 0, 0, 0));
        txtSearch = UITheme.searchField("Tìm theo mã hoặc tên...");
        JButton btnSearch = UITheme.grayButton("Tìm kiếm");
        btnSearch.addActionListener(e -> doSearch());
        txtSearch.addActionListener(e -> doSearch());
        toolbar.add(txtSearch);
        toolbar.add(btnSearch);

        if (ClientSession.getInstance().isAdmin()) {
            JButton btnAdd = UITheme.primaryButton("+ Thêm danh mục");
            btnAdd.addActionListener(e -> openForm(null));
            toolbar.add(btnAdd);
        }
        top.add(toolbar);
        add(top, BorderLayout.NORTH);

        model = new DefaultTableModel(new String[]{"ID", "Mã DM", "Tên danh mục", "Mô tả", "Trạng thái"}, 0) {
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

        if (ClientSession.getInstance().isAdmin()) {
            JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
            bottom.setOpaque(false);
            JButton btnEdit = UITheme.grayButton("Sửa");
            btnEdit.addActionListener(e -> editSelected());
            JButton btnDelete = UITheme.dangerButton("Xoá");
            btnDelete.addActionListener(e -> deleteSelected());
            bottom.add(btnEdit);
            bottom.add(btnDelete);
            add(bottom, BorderLayout.SOUTH);
        }

        refreshData();
    }

    @Override
    public void refreshData() {
        loadData("CATEGORY_FIND_ALL", "");
    }

    private void doSearch() {
        String kw = txtSearch.getText().trim();
        if (kw.isEmpty()) { refreshData(); return; }
        loadData("CATEGORY_SEARCH", kw);
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
                            JsonObject c = el.getAsJsonObject();
                            model.addRow(new Object[]{
                                    c.get("id").getAsInt(),
                                    c.get("code").getAsString(),
                                    c.get("name").getAsString(),
                                    c.has("description") && !c.get("description").isJsonNull() ? c.get("description").getAsString() : "",
                                    c.get("status").getAsString()
                            });
                        }
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(CategoryPanel.this, "Lỗi tải dữ liệu: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void editSelected() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Vui lòng chọn một danh mục"); return; }
        JsonObject c = new JsonObject();
        c.addProperty("id", (Integer) model.getValueAt(row, 0));
        c.addProperty("code", (String) model.getValueAt(row, 1));
        c.addProperty("name", (String) model.getValueAt(row, 2));
        c.addProperty("description", (String) model.getValueAt(row, 3));
        c.addProperty("status", (String) model.getValueAt(row, 4));
        openForm(c);
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Vui lòng chọn một danh mục"); return; }
        int confirm = JOptionPane.showConfirmDialog(this, "Xoá danh mục \"" + model.getValueAt(row, 2) + "\"?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        int id = (Integer) model.getValueAt(row, 0);
        SwingWorker<JsonObject, Void> worker = new SwingWorker<>() {
            @Override protected JsonObject doInBackground() { return ApiClient.getInstance().call("CATEGORY_DELETE", id); }
            @Override protected void done() {
                try {
                    JsonObject resp = get();
                    JOptionPane.showMessageDialog(CategoryPanel.this, resp.get("message").getAsString());
                    refreshData();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(CategoryPanel.this, "Lỗi: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    /**
     * Form Thêm/Sửa danh mục dùng GridBagLayout (label trái - field phải)
     * bọc trong JScrollPane, cùng pattern với ProductPanel, để form luôn
     * gọn, dễ điền, và cuộn được nếu màn hình nhỏ.
     */
    private void openForm(JsonObject existing) {
        boolean isEdit = existing != null;
        JTextField txtCode = new JTextField(isEdit ? existing.get("code").getAsString() : "", 20);
        txtCode.setEnabled(!isEdit);
        JTextField txtName = new JTextField(isEdit ? existing.get("name").getAsString() : "", 20);
        JTextArea txtDesc = new JTextArea(isEdit && existing.has("description") ? existing.get("description").getAsString() : "", 3, 20);
        txtDesc.setLineWrap(true);
        txtDesc.setWrapStyleWord(true);
        JComboBox<String> cbStatus = new JComboBox<>(new String[]{"ACTIVE", "INACTIVE"});
        if (isEdit) cbStatus.setSelectedItem(existing.get("status").getAsString());

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;
        row = addFormRow(form, gbc, row, "Mã danh mục:", txtCode);
        row = addFormRow(form, gbc, row, "Tên danh mục:", txtName);

        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        form.add(new JLabel("Mô tả:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.BOTH; gbc.weightx = 1;
        JScrollPane descScroll = new JScrollPane(txtDesc);
        descScroll.setPreferredSize(new Dimension(220, 60));
        form.add(descScroll, gbc);
        row++;

        if (isEdit) {
            row = addFormRow(form, gbc, row, "Trạng thái:", cbStatus);
        }

        JScrollPane formScroll = new JScrollPane(form);
        formScroll.setBorder(BorderFactory.createEmptyBorder());
        formScroll.getVerticalScrollBar().setUnitIncrement(16);
        formScroll.setPreferredSize(new Dimension(380, Math.min(form.getPreferredSize().height + 20, 420)));

        int result = JOptionPane.showConfirmDialog(this, formScroll, isEdit ? "Sửa danh mục" : "Thêm danh mục",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        if (txtCode.getText().isBlank() || txtName.getText().isBlank()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ mã và tên danh mục");
            return;
        }

        JsonObject data = new JsonObject();
        if (isEdit) data.addProperty("id", existing.get("id").getAsInt());
        data.addProperty("code", txtCode.getText().trim());
        data.addProperty("name", txtName.getText().trim());
        data.addProperty("description", txtDesc.getText().trim());
        if (isEdit) data.addProperty("status", (String) cbStatus.getSelectedItem());

        String action = isEdit ? "CATEGORY_UPDATE" : "CATEGORY_CREATE";
        SwingWorker<JsonObject, Void> worker = new SwingWorker<>() {
            @Override protected JsonObject doInBackground() { return ApiClient.getInstance().call(action, data); }
            @Override protected void done() {
                try {
                    JsonObject resp = get();
                    JOptionPane.showMessageDialog(CategoryPanel.this, resp.get("message").getAsString());
                    if (resp.get("success").getAsBoolean()) refreshData();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(CategoryPanel.this, "Lỗi: " + ex.getMessage());
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
}