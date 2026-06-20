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

public class UserPanel extends JPanel implements MainFrame.RefreshablePanel {

    private DefaultTableModel model;
    private JTable table;
    private JTextField txtSearch;

    public UserPanel() {
        setLayout(new BorderLayout());
        setBackground(UITheme.BG_MAIN);
        setBorder(new EmptyBorder(24, 28, 24, 28));

        // Title và toolbar đặt trên 2 dòng riêng, tránh đè chồng khi cửa sổ hẹp.
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.add(UITheme.sectionTitleWithIcon("\uD83D\uDC64", "Quản lý tài khoản"), BorderLayout.WEST);
        top.add(titleRow);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolbar.setOpaque(false);
        toolbar.setBorder(new EmptyBorder(8, 0, 0, 0));
        txtSearch = UITheme.searchField("Tìm theo username, họ tên...");
        JButton btnSearch = UITheme.grayButton("Tìm kiếm");
        btnSearch.addActionListener(e -> doSearch());
        txtSearch.addActionListener(e -> doSearch());
        toolbar.add(txtSearch);
        toolbar.add(btnSearch);

        JButton btnAdd = UITheme.primaryButton("+ Thêm tài khoản");
        btnAdd.addActionListener(e -> openForm());
        toolbar.add(btnAdd);
        top.add(toolbar);
        add(top, BorderLayout.NORTH);

        model = new DefaultTableModel(new String[]{"ID", "Username", "Họ tên", "Email", "SĐT", "Vai trò", "Trạng thái"}, 0) {
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
        JButton btnLock = UITheme.dangerButton("Khoá tài khoản");
        btnLock.addActionListener(e -> lockSelected());
        JButton btnUnlock = UITheme.successButton("Mở khoá");
        btnUnlock.addActionListener(e -> unlockSelected());
        JButton btnResetPwd = UITheme.grayButton("Đặt lại mật khẩu");
        btnResetPwd.addActionListener(e -> resetPassword());
        bottom.add(btnLock);
        bottom.add(btnUnlock);
        bottom.add(btnResetPwd);
        add(bottom, BorderLayout.SOUTH);

        refreshData();
    }

    @Override
    public void refreshData() {
        loadData("USER_FIND_ALL", "");
    }

    private void doSearch() {
        String kw = txtSearch.getText().trim();
        if (kw.isEmpty()) { refreshData(); return; }
        loadData("USER_SEARCH", kw);
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
                            JsonObject u = el.getAsJsonObject();
                            model.addRow(new Object[]{
                                    u.get("id").getAsInt(),
                                    u.get("username").getAsString(),
                                    u.get("fullName").getAsString(),
                                    str(u, "email"),
                                    str(u, "phone"),
                                    u.get("role").getAsString(),
                                    u.get("status").getAsString()
                            });
                        }
                    } else {
                        JOptionPane.showMessageDialog(UserPanel.this, resp.get("message").getAsString());
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(UserPanel.this, "Lỗi tải dữ liệu: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private String str(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : "";
    }

    private void lockSelected() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Vui lòng chọn một tài khoản"); return; }
        String username = (String) model.getValueAt(row, 1);
        if ("admin".equals(username)) { JOptionPane.showMessageDialog(this, "Không thể khoá tài khoản admin chính"); return; }
        int confirm = JOptionPane.showConfirmDialog(this, "Khoá tài khoản \"" + username + "\"?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        int id = (Integer) model.getValueAt(row, 0);
        SwingWorker<JsonObject, Void> worker = new SwingWorker<>() {
            @Override protected JsonObject doInBackground() { return ApiClient.getInstance().call("USER_DELETE", id); }
            @Override protected void done() {
                try {
                    JsonObject resp = get();
                    JOptionPane.showMessageDialog(UserPanel.this, resp.get("message").getAsString());
                    refreshData();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(UserPanel.this, "Lỗi: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void unlockSelected() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Vui lòng chọn một tài khoản"); return; }
        String username = (String) model.getValueAt(row, 1);
        String status = (String) model.getValueAt(row, 6);
        if (!"LOCKED".equals(status)) { JOptionPane.showMessageDialog(this, "Tài khoản \"" + username + "\" không bị khoá"); return; }
        int confirm = JOptionPane.showConfirmDialog(this, "Mở khoá tài khoản \"" + username + "\"?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        int id = (Integer) model.getValueAt(row, 0);
        SwingWorker<JsonObject, Void> worker = new SwingWorker<>() {
            @Override protected JsonObject doInBackground() { return ApiClient.getInstance().call("USER_UNLOCK", id); }
            @Override protected void done() {
                try {
                    JsonObject resp = get();
                    JOptionPane.showMessageDialog(UserPanel.this, resp.get("message").getAsString());
                    refreshData();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(UserPanel.this, "Lỗi: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void resetPassword() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Vui lòng chọn một tài khoản"); return; }
        int id = (Integer) model.getValueAt(row, 0);
        String username = (String) model.getValueAt(row, 1);

        JPasswordField pwdField = new JPasswordField();
        int result = JOptionPane.showConfirmDialog(this, pwdField, "Đặt mật khẩu mới cho \"" + username + "\" (tối thiểu 6 ký tự)",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        String newPassword = new String(pwdField.getPassword());
        if (newPassword.length() < 6) { JOptionPane.showMessageDialog(this, "Mật khẩu phải ít nhất 6 ký tự"); return; }

        JsonObject data = new JsonObject();
        data.addProperty("userId", id);
        data.addProperty("newPassword", newPassword);
        SwingWorker<JsonObject, Void> worker = new SwingWorker<>() {
            @Override protected JsonObject doInBackground() { return ApiClient.getInstance().call("USER_CHANGE_PASSWORD", data); }
            @Override protected void done() {
                try {
                    JsonObject resp = get();
                    JOptionPane.showMessageDialog(UserPanel.this, resp.get("message").getAsString());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(UserPanel.this, "Lỗi: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    /**
     * Form Thêm tài khoản dùng GridBagLayout (label trái - field phải),
     * đồng bộ với ProductPanel/CategoryPanel/SupplierPanel.
     */
    private void openForm() {
        JTextField txtUsername = new JTextField(20);
        JPasswordField txtPassword = new JPasswordField(20);
        JTextField txtFullName = new JTextField(20);
        JTextField txtEmail = new JTextField(20);
        JTextField txtPhone = new JTextField(20);
        JComboBox<String> cbRole = new JComboBox<>(new String[]{"USER", "ADMIN"});

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;
        row = addFormRow(form, gbc, row, "Tên đăng nhập:", txtUsername);
        row = addFormRow(form, gbc, row, "Mật khẩu (≥6 ký tự):", txtPassword);
        row = addFormRow(form, gbc, row, "Họ tên:", txtFullName);
        row = addFormRow(form, gbc, row, "Email:", txtEmail);
        row = addFormRow(form, gbc, row, "Số điện thoại:", txtPhone);
        row = addFormRow(form, gbc, row, "Vai trò:", cbRole);

        int result = JOptionPane.showConfirmDialog(this, form, "Thêm tài khoản mới",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        if (txtUsername.getText().isBlank() || txtFullName.getText().isBlank()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ tên đăng nhập và họ tên");
            return;
        }
        String password = new String(txtPassword.getPassword());
        if (password.length() < 6) { JOptionPane.showMessageDialog(this, "Mật khẩu phải ít nhất 6 ký tự"); return; }

        JsonObject data = new JsonObject();
        data.addProperty("username", txtUsername.getText().trim());
        data.addProperty("password", password);
        data.addProperty("fullName", txtFullName.getText().trim());
        data.addProperty("email", txtEmail.getText().trim());
        data.addProperty("phone", txtPhone.getText().trim());
        data.addProperty("role", (String) cbRole.getSelectedItem());
        data.addProperty("status", "ACTIVE");

        SwingWorker<JsonObject, Void> worker = new SwingWorker<>() {
            @Override protected JsonObject doInBackground() { return ApiClient.getInstance().call("USER_CREATE", data); }
            @Override protected void done() {
                try {
                    JsonObject resp = get();
                    JOptionPane.showMessageDialog(UserPanel.this, resp.get("message").getAsString());
                    if (resp.get("success").getAsBoolean()) refreshData();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(UserPanel.this, "Lỗi: " + ex.getMessage());
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