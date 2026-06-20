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

public class SupplierPanel extends JPanel implements MainFrame.RefreshablePanel {

    private DefaultTableModel model;
    private JTable table;
    private JTextField txtSearch;

    public SupplierPanel() {
        setLayout(new BorderLayout());
        setBackground(UITheme.BG_MAIN);
        setBorder(new EmptyBorder(24, 28, 24, 28));

        // Title và toolbar đặt trên 2 dòng riêng, tránh đè chồng khi cửa sổ hẹp.
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.add(UITheme.sectionTitleWithIcon("\uD83D\uDE9A", "Quản lý nhà cung cấp"), BorderLayout.WEST);
        top.add(titleRow);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolbar.setOpaque(false);
        toolbar.setBorder(new EmptyBorder(8, 0, 0, 0));
        txtSearch = UITheme.searchField("Tìm theo mã, tên, người liên hệ...");
        JButton btnSearch = UITheme.grayButton("Tìm kiếm");
        btnSearch.addActionListener(e -> doSearch());
        txtSearch.addActionListener(e -> doSearch());
        toolbar.add(txtSearch);
        toolbar.add(btnSearch);

        if (ClientSession.getInstance().isAdmin()) {
            JButton btnAdd = UITheme.primaryButton("+ Thêm NCC");
            btnAdd.addActionListener(e -> openForm(null));
            toolbar.add(btnAdd);
        }
        top.add(toolbar);
        add(top, BorderLayout.NORTH);

        model = new DefaultTableModel(new String[]{"ID", "Mã NCC", "Tên nhà cung cấp", "Người liên hệ", "Điện thoại", "Email", "Địa chỉ"}, 0) {
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
        loadData("SUPPLIER_FIND_ALL", "");
    }

    private void doSearch() {
        String kw = txtSearch.getText().trim();
        if (kw.isEmpty()) { refreshData(); return; }
        loadData("SUPPLIER_SEARCH", kw);
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
                            JsonObject s = el.getAsJsonObject();
                            model.addRow(new Object[]{
                                    s.get("id").getAsInt(),
                                    s.get("code").getAsString(),
                                    s.get("name").getAsString(),
                                    str(s, "contact"),
                                    str(s, "phone"),
                                    str(s, "email"),
                                    str(s, "address")
                            });
                        }
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(SupplierPanel.this, "Lỗi tải dữ liệu: " + ex.getMessage());
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
        if (row < 0) { JOptionPane.showMessageDialog(this, "Vui lòng chọn một nhà cung cấp"); return; }
        JsonObject s = new JsonObject();
        s.addProperty("id", (Integer) model.getValueAt(row, 0));
        s.addProperty("code", (String) model.getValueAt(row, 1));
        s.addProperty("name", (String) model.getValueAt(row, 2));
        s.addProperty("contact", (String) model.getValueAt(row, 3));
        s.addProperty("phone", (String) model.getValueAt(row, 4));
        s.addProperty("email", (String) model.getValueAt(row, 5));
        s.addProperty("address", (String) model.getValueAt(row, 6));
        s.addProperty("status", "ACTIVE");
        openForm(s);
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Vui lòng chọn một nhà cung cấp"); return; }
        int confirm = JOptionPane.showConfirmDialog(this, "Xoá nhà cung cấp \"" + model.getValueAt(row, 2) + "\"?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        int id = (Integer) model.getValueAt(row, 0);
        SwingWorker<JsonObject, Void> worker = new SwingWorker<>() {
            @Override protected JsonObject doInBackground() { return ApiClient.getInstance().call("SUPPLIER_DELETE", id); }
            @Override protected void done() {
                try {
                    JsonObject resp = get();
                    JOptionPane.showMessageDialog(SupplierPanel.this, resp.get("message").getAsString());
                    refreshData();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(SupplierPanel.this, "Lỗi: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    /**
     * Form Thêm/Sửa nhà cung cấp dùng GridBagLayout (label trái - field phải)
     * bọc trong JScrollPane, cùng pattern với ProductPanel/CategoryPanel,
     * để form luôn gọn, dễ điền, và cuộn được nếu màn hình nhỏ.
     */
    private void openForm(JsonObject existing) {
        boolean isEdit = existing != null;
        JTextField txtCode = new JTextField(isEdit ? existing.get("code").getAsString() : "", 20);
        txtCode.setEnabled(!isEdit);
        JTextField txtName = new JTextField(isEdit ? existing.get("name").getAsString() : "", 20);
        JTextField txtContact = new JTextField(isEdit ? str(existing, "contact") : "", 20);
        JTextField txtPhone = new JTextField(isEdit ? str(existing, "phone") : "", 20);
        JTextField txtEmail = new JTextField(isEdit ? str(existing, "email") : "", 20);
        JTextArea txtAddress = new JTextArea(isEdit ? str(existing, "address") : "", 2, 20);
        txtAddress.setLineWrap(true);
        txtAddress.setWrapStyleWord(true);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;
        row = addFormRow(form, gbc, row, "Mã NCC:", txtCode);
        row = addFormRow(form, gbc, row, "Tên nhà cung cấp:", txtName);
        row = addFormRow(form, gbc, row, "Người liên hệ:", txtContact);
        row = addFormRow(form, gbc, row, "Điện thoại:", txtPhone);
        row = addFormRow(form, gbc, row, "Email:", txtEmail);

        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        form.add(new JLabel("Địa chỉ:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.BOTH; gbc.weightx = 1;
        JScrollPane addressScroll = new JScrollPane(txtAddress);
        addressScroll.setPreferredSize(new Dimension(220, 50));
        form.add(addressScroll, gbc);

        JScrollPane formScroll = new JScrollPane(form);
        formScroll.setBorder(BorderFactory.createEmptyBorder());
        formScroll.getVerticalScrollBar().setUnitIncrement(16);
        formScroll.setPreferredSize(new Dimension(380, Math.min(form.getPreferredSize().height + 20, 420)));

        int result = JOptionPane.showConfirmDialog(this, formScroll, isEdit ? "Sửa nhà cung cấp" : "Thêm nhà cung cấp",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        if (txtCode.getText().isBlank() || txtName.getText().isBlank()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ mã và tên nhà cung cấp");
            return;
        }

        JsonObject data = new JsonObject();
        if (isEdit) data.addProperty("id", existing.get("id").getAsInt());
        data.addProperty("code", txtCode.getText().trim());
        data.addProperty("name", txtName.getText().trim());
        data.addProperty("contact", txtContact.getText().trim());
        data.addProperty("phone", txtPhone.getText().trim());
        data.addProperty("email", txtEmail.getText().trim());
        data.addProperty("address", txtAddress.getText().trim());
        if (isEdit) data.addProperty("status", "ACTIVE");

        String action = isEdit ? "SUPPLIER_UPDATE" : "SUPPLIER_CREATE";
        SwingWorker<JsonObject, Void> worker = new SwingWorker<>() {
            @Override protected JsonObject doInBackground() { return ApiClient.getInstance().call(action, data); }
            @Override protected void done() {
                try {
                    JsonObject resp = get();
                    JOptionPane.showMessageDialog(SupplierPanel.this, resp.get("message").getAsString());
                    if (resp.get("success").getAsBoolean()) refreshData();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(SupplierPanel.this, "Lỗi: " + ex.getMessage());
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