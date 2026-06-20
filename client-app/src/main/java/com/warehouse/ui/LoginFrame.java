package com.warehouse.ui;

import com.google.gson.JsonObject;
import com.warehouse.service.ApiClient;
import com.warehouse.service.ClientSession;
import com.warehouse.service.ServerConnection;
import com.warehouse.util.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;

public class LoginFrame extends JFrame {

    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JLabel lblMessage;
    private JButton btnLogin;

    public LoginFrame() {
        setTitle("Đăng nhập - Quản lý kho Linh kiện Máy tính");
        setSize(420, 480);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UITheme.BG_WHITE);

        // Header
        JPanel header = new JPanel();
        header.setBackground(UITheme.PRIMARY);
        header.setPreferredSize(new Dimension(420, 110));
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        JLabel lblIcon = new JLabel(" KHO LINH KIỆN MÁY TÍNH");
        lblIcon.setIcon(new UITheme.EmojiIcon("\uD83D\uDDA5\uFE0F", 20, Color.WHITE));
        lblIcon.setIconTextGap(8);
        lblIcon.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblIcon.setForeground(Color.WHITE);
        lblIcon.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel lblSub = new JLabel("Hệ thống quản lý kho hàng");
        lblSub.setFont(UITheme.FONT_NORMAL);
        lblSub.setForeground(new Color(220, 230, 255));
        lblSub.setAlignmentX(Component.CENTER_ALIGNMENT);
        header.add(Box.createVerticalGlue());
        header.add(lblIcon);
        header.add(Box.createVerticalStrut(6));
        header.add(lblSub);
        header.add(Box.createVerticalGlue());
        root.add(header, BorderLayout.NORTH);

        // Form
        JPanel form = new JPanel();
        form.setBorder(new EmptyBorder(35, 45, 35, 45));
        form.setBackground(UITheme.BG_WHITE);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        JLabel lblUser = new JLabel("Tên đăng nhập");
        lblUser.setFont(UITheme.FONT_BOLD);
        lblUser.setAlignmentX(Component.LEFT_ALIGNMENT);

        txtUsername = new JTextField();
        txtUsername.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        txtUsername.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1, true), new EmptyBorder(6, 10, 6, 10)));
        txtUsername.setAlignmentX(Component.LEFT_ALIGNMENT);
        txtUsername.setText("admin");

        JLabel lblPass = new JLabel("Mật khẩu");
        lblPass.setFont(UITheme.FONT_BOLD);
        lblPass.setAlignmentX(Component.LEFT_ALIGNMENT);

        txtPassword = new JPasswordField();
        txtPassword.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        txtPassword.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1, true), new EmptyBorder(6, 10, 6, 10)));
        txtPassword.setAlignmentX(Component.LEFT_ALIGNMENT);
        txtPassword.setText("Admin@123");

        lblMessage = new JLabel(" ");
        lblMessage.setForeground(UITheme.DANGER);
        lblMessage.setFont(UITheme.FONT_SMALL);
        lblMessage.setAlignmentX(Component.LEFT_ALIGNMENT);

        btnLogin = UITheme.primaryButton("ĐĂNG NHẬP");
        btnLogin.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnLogin.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        btnLogin.addActionListener(this::doLogin);

        JLabel lblHint = new JLabel("<html><center>Demo: admin/Admin@123 (Admin)<br>user01/User@123 (Nhân viên)</center></html>");
        lblHint.setFont(UITheme.FONT_SMALL);
        lblHint.setForeground(Color.GRAY);
        lblHint.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblServer = new JLabel("Server: " + ServerConnection.getHost() + ":" + ServerConnection.getPort());
        lblServer.setFont(UITheme.FONT_SMALL);
        lblServer.setForeground(Color.GRAY);
        lblServer.setAlignmentX(Component.LEFT_ALIGNMENT);

        form.add(lblUser);
        form.add(Box.createVerticalStrut(6));
        form.add(txtUsername);
        form.add(Box.createVerticalStrut(16));
        form.add(lblPass);
        form.add(Box.createVerticalStrut(6));
        form.add(txtPassword);
        form.add(Box.createVerticalStrut(10));
        form.add(lblMessage);
        form.add(Box.createVerticalStrut(10));
        form.add(btnLogin);
        form.add(Box.createVerticalStrut(20));
        form.add(lblHint);
        form.add(Box.createVerticalStrut(8));
        form.add(lblServer);

        root.add(form, BorderLayout.CENTER);
        setContentPane(root);

        getRootPane().setDefaultButton(btnLogin);
        txtPassword.addActionListener(this::doLogin);
    }

    private void doLogin(ActionEvent e) {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            lblMessage.setText("Vui lòng nhập đầy đủ thông tin");
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("ĐANG XỬ LÝ...");
        lblMessage.setText(" ");

        SwingWorker<JsonObject, Void> worker = new SwingWorker<>() {
            @Override
            protected JsonObject doInBackground() {
                JsonObject creds = new JsonObject();
                creds.addProperty("username", username);
                creds.addProperty("password", password);
                return ApiClient.getInstance().call("LOGIN", creds);
            }

            @Override
            protected void done() {
                btnLogin.setEnabled(true);
                btnLogin.setText("ĐĂNG NHẬP");
                try {
                    JsonObject resp = get();
                    boolean success = resp.has("success") && !resp.get("success").isJsonNull() && resp.get("success").getAsBoolean();
                    if (success && resp.has("data") && resp.get("data").isJsonObject()) {
                        JsonObject data = resp.getAsJsonObject("data");
                        int id = data.get("id").getAsInt();
                        String uname = data.get("username").getAsString();
                        String fullName = data.get("fullName").getAsString();
                        String role = data.get("role").getAsString();
                        String token = data.get("password").getAsString(); // token gửi tạm qua field password

                        ClientSession.getInstance().login(id, uname, fullName, role, token);

                        MainFrame mainFrame = new MainFrame();
                        mainFrame.setVisible(true);
                        dispose();
                    } else {
                        String msg = resp.has("message") && !resp.get("message").isJsonNull()
                                ? resp.get("message").getAsString() : "Đăng nhập thất bại";
                        lblMessage.setText(msg);
                    }
                } catch (Exception ex) {
                    lblMessage.setText("Lỗi kết nối tới server, vui lòng thử lại");
                }
            }
        };
        worker.execute();
    }
}