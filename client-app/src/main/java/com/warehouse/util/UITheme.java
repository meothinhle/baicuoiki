package com.warehouse.util;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;

public class UITheme {
    // Colors
    public static final Color PRIMARY   = new Color(25, 118, 210);
    public static final Color PRIMARY_DARK = new Color(13, 71, 161);
    public static final Color SUCCESS   = new Color(46, 125, 50);
    public static final Color DANGER    = new Color(198, 40, 40);
    public static final Color WARNING   = new Color(245, 124, 0);
    public static final Color BG_MAIN   = new Color(245, 247, 250);
    public static final Color BG_WHITE  = Color.WHITE;
    public static final Color BG_SIDEBAR= new Color(21, 34, 56);
    public static final Color TEXT_SIDEBAR = new Color(178, 198, 230);
    public static final Color TEXT_MAIN = new Color(33, 33, 33);
    public static final Color BORDER    = new Color(218, 220, 224);
    public static final Color ROW_ODD   = new Color(250, 251, 252);
    public static final Color ROW_EVEN  = Color.WHITE;
    public static final Color ROW_LOW   = new Color(255, 243, 224);
    public static final Color HEADER_BG = new Color(25, 118, 210);
    public static final Color HEADER_FG = Color.WHITE;

    // Fonts
    public static final Font FONT_TITLE  = new Font("Segoe UI", Font.BOLD, 20);
    public static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_NORMAL = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_SMALL  = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_BOLD   = new Font("Segoe UI", Font.BOLD, 13);
    // Font dự phòng để vẽ emoji icon (Windows: Segoe UI Emoji, Linux: Noto Color Emoji)
    public static final Font FONT_EMOJI  = new Font("Segoe UI Emoji", Font.PLAIN, 14);

    public static void applyGlobalTheme() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        UIManager.put("Button.font", FONT_NORMAL);
        UIManager.put("Label.font", FONT_NORMAL);
        UIManager.put("TextField.font", FONT_NORMAL);
        UIManager.put("TextArea.font", FONT_NORMAL);
        UIManager.put("ComboBox.font", FONT_NORMAL);
        UIManager.put("Table.font", FONT_NORMAL);
        UIManager.put("TableHeader.font", FONT_HEADER);
        UIManager.put("OptionPane.messageFont", FONT_NORMAL);
        UIManager.put("OptionPane.buttonFont", FONT_NORMAL);
    }

    public static JButton primaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(PRIMARY);
        btn.setForeground(Color.WHITE);
        btn.setFont(FONT_BOLD);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 18, 8, 18));
        return btn;
    }

    public static JButton dangerButton(String text) {
        JButton btn = primaryButton(text);
        btn.setBackground(DANGER);
        return btn;
    }

    public static JButton successButton(String text) {
        JButton btn = primaryButton(text);
        btn.setBackground(SUCCESS);
        return btn;
    }

    public static JButton grayButton(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(new Color(120,120,120));
        btn.setForeground(Color.WHITE);
        btn.setFont(FONT_BOLD);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 18, 8, 18));
        return btn;
    }

    public static void styleTable(JTable table) {
        table.setFont(FONT_NORMAL);
        table.setRowHeight(32);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(new Color(187, 222, 251));
        table.setSelectionForeground(TEXT_MAIN);
        table.setGridColor(BORDER);
        table.setFillsViewportHeight(true);

        JTableHeader header = table.getTableHeader();
        header.setPreferredSize(new Dimension(0, 40));
        header.setReorderingAllowed(false);
        header.setResizingAllowed(true);
        // Dùng custom renderer ép màu/font header, vì set trực tiếp lên
        // JTableHeader bị Look-and-Feel native (ví dụ Windows L&F) bỏ qua.
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            { setHorizontalAlignment(LEFT); }
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val, boolean sel, boolean focus, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(tbl, val, sel, focus, row, col);
                lbl.setOpaque(true);
                lbl.setBackground(HEADER_BG);
                lbl.setForeground(HEADER_FG);
                lbl.setFont(FONT_HEADER);
                lbl.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 2, 0, PRIMARY_DARK),
                        new EmptyBorder(0, 12, 0, 12)));
                return lbl;
            }
        });

        // Stripe rows + padding cho từng cell dữ liệu
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val, boolean sel, boolean focus, int row, int col) {
                Component c = super.getTableCellRendererComponent(tbl, val, sel, focus, row, col);
                c.setForeground(TEXT_MAIN);
                if (!sel) c.setBackground(row % 2 == 0 ? ROW_EVEN : ROW_ODD);
                setBorder(new EmptyBorder(0, 12, 0, 12));
                return c;
            }
        });
    }

    public static JTextField searchField(String placeholder) {
        JTextField field = new JTextField(20);
        field.setFont(FONT_NORMAL);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                new EmptyBorder(6, 10, 6, 10)
        ));
        field.setToolTipText(placeholder);
        return field;
    }

    public static JLabel sectionTitle(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_TITLE);
        lbl.setForeground(PRIMARY_DARK);
        lbl.setBorder(new EmptyBorder(0, 0, 8, 0));
        return lbl;
    }

    /**
     * Icon vẽ emoji bằng Graphics2D vào 1 vùng kích thước CỐ ĐỊNH, dùng qua
     * setIcon() trên JLabel/JButton. Tránh hoàn toàn các vấn đề khi ghép
     * emoji (JLabel/text) với text khác trong cùng component: không wrap,
     * không lệch theo độ rộng tự nhiên của từng emoji, không phụ thuộc
     * cách Look-and-Feel native vẽ lại component.
     * Ví dụ: label.setIcon(new UITheme.EmojiIcon("\uD83D\uDDA5\uFE0F", 22, Color.WHITE));
     */
    public static class EmojiIcon implements Icon {
        private final String emoji;
        private final int size;
        private final Color tint;

        public EmojiIcon(String emoji, int size, Color tint) {
            this.emoji = emoji;
            this.size = size;
            this.tint = tint;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Font emojiFont = FONT_EMOJI.deriveFont((float) size);
            g2.setFont(emojiFont);
            FontMetrics fm = g2.getFontMetrics();
            int textWidth = fm.stringWidth(emoji);
            int drawX = x + (getIconWidth() - textWidth) / 2;
            int drawY = y + fm.getAscent() + (getIconHeight() - fm.getHeight()) / 2;
            g2.setColor(tint);
            g2.drawString(emoji, drawX, drawY);
            g2.dispose();
        }

        @Override public int getIconWidth() { return size + 6; }
        @Override public int getIconHeight() { return size + 4; }
    }

    /**
     * Tạo section title có icon emoji + text tách riêng font, tránh trường hợp
     * emoji hiện thành ô vuông khi font chữ chính (Segoe UI) không có glyph emoji.
     * Ví dụ: sectionTitleWithIcon("\uD83D\uDC64", "Quản lý tài khoản")
     */
    public static JPanel sectionTitleWithIcon(String emoji, String text) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setBorder(new EmptyBorder(0, 0, 8, 0));

        JLabel lblIcon = new JLabel(emoji);
        lblIcon.setFont(FONT_EMOJI.deriveFont(20f));

        JLabel lblText = new JLabel(text);
        lblText.setFont(FONT_TITLE);
        lblText.setForeground(PRIMARY_DARK);
        lblText.setBorder(new EmptyBorder(0, 8, 0, 0));

        panel.add(lblIcon);
        panel.add(lblText);
        return panel;
    }
}