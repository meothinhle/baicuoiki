package com.warehouse.ui;

import com.warehouse.service.ApiClient;
import com.warehouse.service.ClientSession;
import com.warehouse.ui.panels.*;
import com.warehouse.util.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class MainFrame extends JFrame {

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(cardLayout);
    private final Map<String, JButton> navButtons = new LinkedHashMap<>();
    private final Map<String, String> navEmojis = new LinkedHashMap<>();
    private String activeCard = "DASHBOARD";

    public MainFrame() {
        ClientSession session = ClientSession.getInstance();

        setTitle("Quản lý kho Linh kiện Máy tính - " + session.getFullName() + " (" + session.getRole() + ")");
        setSize(900, 600);
        setMinimumSize(new Dimension(600, 400));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout());

        // ===== Sidebar =====
        JPanel sidebar = buildSidebar(session);
        root.add(sidebar, BorderLayout.WEST);

        // ===== Content =====
        contentPanel.setBackground(UITheme.BG_MAIN);
        contentPanel.add(new DashboardPanel(), "DASHBOARD");
        contentPanel.add(new ProductPanel(), "PRODUCT");
        contentPanel.add(new OrderChartPanel(), "CHARTS");
        contentPanel.add(new CategoryPanel(), "CATEGORY");
        contentPanel.add(new SupplierPanel(), "SUPPLIER");
        contentPanel.add(new ImportPanel(), "IMPORT");
        contentPanel.add(new ExportPanel(), "EXPORT");
        if (session.isAdmin()) {
            contentPanel.add(new UserPanel(), "USERS");
            contentPanel.add(new LogPanel(), "LOGS");
        }
        root.add(contentPanel, BorderLayout.CENTER);

        setContentPane(root);
        showCard("DASHBOARD");
    }

    private JPanel buildSidebar(ClientSession session) {
        JPanel sidebar = new JPanel();
        // Ép cứng width 230px bằng cả 3 size (preferred/min/max), vì chỉ set
        // preferredSize không đủ: BoxLayout có thể vẫn giãn sidebar theo
        // component con nếu Look-and-Feel native (nút active màu xanh) cố
        // giãn theo nội dung, gây tràn highlight ra ngoài vùng sidebar.
        Dimension sidebarSize = new Dimension(230, 0);
        sidebar.setPreferredSize(sidebarSize);
        sidebar.setMinimumSize(sidebarSize);
        sidebar.setMaximumSize(new Dimension(230, Integer.MAX_VALUE));
        sidebar.setBackground(UITheme.BG_SIDEBAR);
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));

        JPanel logo = new JPanel();
        logo.setBackground(UITheme.BG_SIDEBAR);
        logo.setMaximumSize(new Dimension(230, 70));
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);
        logo.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
        logo.setBorder(new EmptyBorder(15, 10, 15, 10));

        // Icon vẽ bằng Icon/Graphics2D (EmojiIcon) thay vì JLabel text,
        // tránh hoàn toàn vấn đề font-rendering / wrap / lệch layout khi
        // ghép icon emoji với text trong cùng 1 component Swing.
        JLabel lblLogo = new JLabel(" KHO LINH KIỆN");
        lblLogo.setIcon(new UITheme.EmojiIcon("\uD83D\uDDA5\uFE0F", 22, Color.WHITE));
        lblLogo.setIconTextGap(8);
        lblLogo.setForeground(Color.WHITE);
        lblLogo.setFont(new Font("Segoe UI", Font.BOLD, 15));
        logo.add(lblLogo);

        sidebar.add(logo);
        sidebar.add(separator());

        addNavButton(sidebar, "DASHBOARD", "\uD83D\uDCCA", "Tổng quan");
        addNavButton(sidebar, "PRODUCT", "\uD83D\uDCE6", "Sản phẩm");
        addNavButton(sidebar, "CHARTS", "\uD83D\uDCC8", "Biểu đồ");
        addNavButton(sidebar, "CATEGORY", "\uD83D\uDDC2\uFE0F", "Danh mục");
        addNavButton(sidebar, "SUPPLIER", "\uD83D\uDE9A", "Nhà cung cấp");
        addNavButton(sidebar, "IMPORT", "\u2B07", "Phiếu nhập kho");
        addNavButton(sidebar, "EXPORT", "\u2B06", "Phiếu xuất kho");
        if (session.isAdmin()) {
            sidebar.add(separator());
            addNavButton(sidebar, "USERS", "\uD83D\uDC64", "Tài khoản");
            addNavButton(sidebar, "LOGS", "\uD83D\uDCDC", "Nhật ký hệ thống");
        }

        sidebar.add(Box.createVerticalGlue());
        sidebar.add(separator());

        JPanel userBox = new JPanel();
        userBox.setBackground(UITheme.BG_SIDEBAR);
        userBox.setLayout(new BoxLayout(userBox, BoxLayout.Y_AXIS));
        userBox.setBorder(new EmptyBorder(12, 18, 16, 18));
        userBox.setMaximumSize(new Dimension(230, 90));
        userBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lblName = new JLabel(session.getFullName());
        lblName.setForeground(Color.WHITE);
        lblName.setFont(UITheme.FONT_BOLD);
        JLabel lblRole = new JLabel(session.isAdmin() ? "Quản trị viên" : "Nhân viên");
        lblRole.setForeground(UITheme.TEXT_SIDEBAR);
        lblRole.setFont(UITheme.FONT_SMALL);
        userBox.add(lblName);
        userBox.add(lblRole);
        userBox.add(Box.createVerticalStrut(8));

        JButton btnLogout = new JButton("Đăng xuất");
        btnLogout.setMaximumSize(new Dimension(195, 32));
        btnLogout.setBackground(UITheme.DANGER);
        btnLogout.setForeground(Color.WHITE);
        btnLogout.setFocusPainted(false);
        btnLogout.setBorderPainted(false);
        btnLogout.setFont(UITheme.FONT_SMALL);
        btnLogout.addActionListener(e -> doLogout());
        userBox.add(btnLogout);

        sidebar.add(userBox);
        return sidebar;
    }

    private JSeparator separator() {
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(50, 65, 90));
        sep.setMaximumSize(new Dimension(230, 1));
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        return sep;
    }

    /**
     * Tạo nav button dùng JButton chuẩn với setIcon()+setText(), KHÔNG lồng
     * JPanel/JLabel con nào cả. Icon được vẽ bằng EmojiIcon (Graphics2D),
     * không phải JLabel text, nên không có vấn đề font-rendering, wrap,
     * hay lệch layout do BoxLayout/BorderLayout chồng lên nhau trên JButton
     * (đây chính là nguyên nhân gây lệch/dồn sang phải ở các lần sửa trước).
     */
    private void addNavButton(JPanel sidebar, String cardName, String emoji, String label) {
        JButton btn = new JButton(label);
        btn.setIcon(new UITheme.EmojiIcon(emoji, 16, UITheme.TEXT_SIDEBAR));
        btn.setIconTextGap(12);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        Dimension btnSize = new Dimension(230, 46);
        btn.setMaximumSize(btnSize);
        btn.setPreferredSize(btnSize);
        btn.setMinimumSize(btnSize);
        // BoxLayout (Y_AXIS) căn chỉnh theo alignmentX của từng component;
        // nếu không set rõ, 1 số Look-and-Feel native có thể không tôn
        // trọng maximumSize khi vẽ lại nút active (màu xanh tràn ra ngoài
        // 230px). Ép alignmentX = LEFT để BoxLayout neo cứng theo width đã set.
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setBackground(UITheme.BG_SIDEBAR);
        btn.setForeground(UITheme.TEXT_SIDEBAR);
        btn.setFont(UITheme.FONT_NORMAL);
        btn.setBorder(new EmptyBorder(0, 22, 0, 0));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(true);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> showCard(cardName));

        sidebar.add(btn);
        navButtons.put(cardName, btn);
        navEmojis.put(cardName, emoji);
    }

    private void showCard(String cardName) {
        activeCard = cardName;
        cardLayout.show(contentPanel, cardName);
        for (Map.Entry<String, JButton> entry : navButtons.entrySet()) {
            boolean active = entry.getKey().equals(cardName);
            JButton btn = entry.getValue();
            Color fg = active ? Color.WHITE : UITheme.TEXT_SIDEBAR;
            btn.setBackground(active ? UITheme.PRIMARY : UITheme.BG_SIDEBAR);
            btn.setForeground(fg);
            btn.setIcon(new UITheme.EmojiIcon(navEmojis.get(entry.getKey()), 16, fg));
        }
        // Refresh panel data mỗi khi chuyển tab
        for (Component comp : contentPanel.getComponents()) {
            if (comp.isVisible() && comp instanceof RefreshablePanel rp) {
                rp.refreshData();
            }
        }
    }

    private void doLogout() {
        int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc muốn đăng xuất?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            ApiClient.getInstance().call("LOGOUT");
            ClientSession.getInstance().logout();
            ApiClient.getInstance().disconnect();
            new LoginFrame().setVisible(true);
            dispose();
        }
    }

    /** Interface để các panel tự refresh dữ liệu khi được hiển thị */
    public interface RefreshablePanel {
        void refreshData();
    }
}