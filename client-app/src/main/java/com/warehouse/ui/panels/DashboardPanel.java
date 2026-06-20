package com.warehouse.ui.panels;

import com.google.gson.JsonObject;
import com.warehouse.service.ApiClient;
import com.warehouse.ui.MainFrame;
import com.warehouse.util.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class DashboardPanel extends JPanel implements MainFrame.RefreshablePanel {

    private JPanel cardsContainer;
    private JTable tableLowStock;
    private DefaultTableModel lowStockModel;
    private JLabel lblLastUpdate;

    public DashboardPanel() {
        setLayout(new BorderLayout());
        setBackground(UITheme.BG_MAIN);
        setBorder(new EmptyBorder(24, 28, 24, 28));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(UITheme.sectionTitleWithIcon("\uD83D\uDCCA", "Tổng quan hệ thống"), BorderLayout.WEST);
        lblLastUpdate = new JLabel();
        lblLastUpdate.setFont(UITheme.FONT_SMALL);
        lblLastUpdate.setForeground(Color.GRAY);
        top.add(lblLastUpdate, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 20));
        center.setOpaque(false);

        cardsContainer = new JPanel(new GridLayout(1, 6, 14, 0));
        cardsContainer.setOpaque(false);
        cardsContainer.setPreferredSize(new Dimension(0, 110));
        center.add(cardsContainer, BorderLayout.NORTH);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);

        // Icon cảnh báo (⚠️) tách riêng font emoji khỏi text, tránh hiện
        // thành ô vuông màu cam khi dùng chung font Segoe UI thường (FONT_BOLD).
        JPanel lowStockHeader = new JPanel();
        lowStockHeader.setOpaque(false);
        lowStockHeader.setLayout(new BoxLayout(lowStockHeader, BoxLayout.X_AXIS));
        lowStockHeader.setBorder(new EmptyBorder(10, 0, 10, 0));
        JLabel lblWarnIcon = new JLabel("\u26A0\uFE0F");
        lblWarnIcon.setFont(UITheme.FONT_EMOJI.deriveFont(14f));
        JLabel lblLow = new JLabel("  Sản phẩm sắp hết hàng (tồn kho ≤ mức tối thiểu)");
        lblLow.setFont(UITheme.FONT_BOLD);
        lblLow.setForeground(UITheme.WARNING);
        lowStockHeader.add(lblWarnIcon);
        lowStockHeader.add(lblLow);
        bottomPanel.add(lowStockHeader, BorderLayout.NORTH);

        lowStockModel = new DefaultTableModel(new String[]{"Mã SP", "Tên sản phẩm", "Danh mục", "Tồn kho", "Mức tối thiểu"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tableLowStock = new JTable(lowStockModel);
        UITheme.styleTable(tableLowStock);
        JScrollPane scroll = new JScrollPane(tableLowStock);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER));
        bottomPanel.add(scroll, BorderLayout.CENTER);

        center.add(bottomPanel, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        refreshData();
    }

    @Override
    public void refreshData() {
        SwingWorker<JsonObject, Void> worker = new SwingWorker<>() {
            @Override
            protected JsonObject doInBackground() {
                return ApiClient.getInstance().call("DASHBOARD_STATS");
            }

            @Override
            protected void done() {
                try {
                    JsonObject resp = get();
                    if (resp.has("success") && resp.get("success").getAsBoolean()) {
                        JsonObject data = resp.getAsJsonObject("data");
                        renderCards(data);
                        renderLowStock(data);
                        lblLastUpdate.setText("Cập nhật lúc: " + java.time.LocalTime.now().withNano(0));
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(DashboardPanel.this, "Lỗi tải dữ liệu: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void renderCards(JsonObject data) {
        cardsContainer.removeAll();
        cardsContainer.add(statCard("\uD83D\uDCE6", "Tổng sản phẩm", String.valueOf(data.get("totalProducts").getAsInt()), UITheme.PRIMARY));
        cardsContainer.add(statCard("\u26A0\uFE0F", "Sắp hết hàng", String.valueOf(data.get("lowStockProducts").getAsInt()), UITheme.WARNING));
        cardsContainer.add(statCard("\uD83D\uDDC2\uFE0F", "Danh mục", String.valueOf(data.get("totalCategories").getAsInt()), UITheme.SUCCESS));
        cardsContainer.add(statCard("\uD83D\uDE9A", "Nhà cung cấp", String.valueOf(data.get("totalSuppliers").getAsInt()), new Color(123, 31, 162)));
        cardsContainer.add(statCard("\u2B07", "Phiếu nhập", String.valueOf(data.get("totalImportOrders").getAsInt()), new Color(0, 121, 107)));
        cardsContainer.add(statCard("\u2B06", "Phiếu xuất", String.valueOf(data.get("totalExportOrders").getAsInt()), UITheme.DANGER));
        cardsContainer.revalidate();
        cardsContainer.repaint();
    }

    private JPanel statCard(String icon, String title, String value, Color color) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER),
                new EmptyBorder(14, 16, 14, 16)));

        JLabel lblIcon = new JLabel(icon);
        lblIcon.setFont(UITheme.FONT_EMOJI.deriveFont(22f));

        JLabel lblValue = new JLabel(value);
        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblValue.setForeground(color);

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(UITheme.FONT_SMALL);
        lblTitle.setForeground(Color.GRAY);

        card.add(lblIcon);
        card.add(Box.createVerticalStrut(4));
        card.add(lblValue);
        card.add(lblTitle);
        return card;
    }

    private void renderLowStock(JsonObject data) {
        lowStockModel.setRowCount(0);
        if (data.has("lowStockList") && !data.get("lowStockList").isJsonNull()) {
            data.getAsJsonArray("lowStockList").forEach(el -> {
                JsonObject row = el.getAsJsonObject();
                lowStockModel.addRow(new Object[]{
                        row.get("code").getAsString(),
                        row.get("name").getAsString(),
                        row.has("categoryName") && !row.get("categoryName").isJsonNull() ? row.get("categoryName").getAsString() : "",
                        row.get("quantity").getAsInt(),
                        row.get("minQuantity").getAsInt()
                });
            });
        }
    }
}