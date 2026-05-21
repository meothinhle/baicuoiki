package Frame;

import DataBase_Sever.DBconnection;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class LoginFrame extends JFrame {
    private JTextField txtUsername;
    private JPasswordField txtPassword, txtConfirmPassword;
    private JButton btnLogin, btnRegister, btnSubmitRegister;
    private JPanel mainPanel;
    private CardLayout cardLayout;

    public LoginFrame() {
        setTitle("Quản lý chi tiêu - Đăng nhập");
        setSize(400, 300);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // --- GIAO DIỆN ĐĂNG NHẬP ---
        JPanel loginPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        loginPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        loginPanel.add(new JLabel("Tên đăng nhập:"));
        txtUsername = new JTextField();
        loginPanel.add(txtUsername);

        loginPanel.add(new JLabel("Mật khẩu:"));
        txtPassword = new JPasswordField();
        loginPanel.add(txtPassword);

        btnRegister = new JButton("Đăng ký");
        loginPanel.add(btnRegister);

        btnLogin = new JButton("Đăng nhập");
        loginPanel.add(btnLogin);

        // --- GIAO DIỆN ĐĂNG KÝ ---
        JPanel registerPanel = new JPanel(new GridLayout(5, 2, 10, 10));
        registerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        txtConfirmPassword = new JPasswordField();
        registerPanel.add(new JLabel("Tên đăng nhập:"));
        JTextField regUser = new JTextField();
        registerPanel.add(regUser);

        registerPanel.add(new JLabel("Mật khẩu:"));
        JPasswordField regPass = new JPasswordField();
        registerPanel.add(regPass);

        registerPanel.add(new JLabel("Xác nhận mật khẩu:"));
        JPasswordField regConfirm = new JPasswordField();
        registerPanel.add(regConfirm);

        JButton btnBack = new JButton("Quay lại");
        registerPanel.add(btnBack);

        btnSubmitRegister = new JButton("Xác nhận ĐK");
        registerPanel.add(btnSubmitRegister);

        // Thêm vào mainPanel
        mainPanel.add(loginPanel, "login");
        mainPanel.add(registerPanel, "register");
        add(mainPanel);

        // --- XỬ LÝ SỰ KIỆN ---

        // Chuyển sang form Đăng ký
        btnRegister.addActionListener(e -> cardLayout.show(mainPanel, "register"));

        // Quay lại Đăng nhập
        btnBack.addActionListener(e -> cardLayout.show(mainPanel, "login"));

        // Logic Đăng ký
        btnSubmitRegister.addActionListener(e -> {
            String user = regUser.getText();
            String pass = new String(regPass.getPassword());
            String confirm = new String(regConfirm.getPassword());

            if (user.isEmpty() || pass.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ!");
            } else if (!pass.equals(confirm)) {
                JOptionPane.showMessageDialog(this, "Mật khẩu xác nhận không khớp!");
            } else {
                handleRegister(user, pass);
            }
        });

        // Logic Đăng nhập
        btnLogin.addActionListener(e -> handleLogin());
    }

    private void handleRegister(String user, String pass) {
        try (Connection conn = DBconnection.getConnection()) {
            if (conn == null) {
                JOptionPane.showMessageDialog(this, "Kết nối Database thất bại! Kiểm tra lại DBConnection.");
                return;
            }
            String sql = "INSERT INTO user (username, password) VALUES (?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, user);
            ps.setString(2, pass);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Đăng ký thành công!");
            cardLayout.show(mainPanel, "login");
        } catch (SQLException ex) {
            // Dòng này rất quan trọng để chẩn đoán lỗi:
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Lỗi cụ thể: " + ex.getMessage());
        }
    }

    private void handleLogin() {
        String user = txtUsername.getText();
        String pass = new String(txtPassword.getPassword());

        try (Connection conn = DBconnection.getConnection()) {
            String sql = "SELECT * FROM user WHERE username=? AND password=?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, user);
            ps.setString(2, pass);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                JOptionPane.showMessageDialog(this, "Đăng nhập thành công!");
                // Tại đây bạn có thể mở giao diện quản lý chi tiêu chính
            } else {
                JOptionPane.showMessageDialog(this, "Sai tài khoản hoặc mật khẩu!");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}