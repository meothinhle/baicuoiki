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

public class LogPanel extends JPanel implements MainFrame.RefreshablePanel {

    private DefaultTableModel model;
    private JTable table;

    public LogPanel() {
        setLayout(new BorderLayout());
        setBackground(UITheme.BG_MAIN);
        setBorder(new EmptyBorder(24, 28, 24, 28));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(UITheme.sectionTitleWithIcon("\uD83D\uDCDC", "Nhật ký hệ thống"), BorderLayout.WEST);

        JButton btnRefresh = UITheme.grayButton("Tải lại");
        btnRefresh.addActionListener(e -> refreshData());
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        toolbar.setOpaque(false);
        toolbar.add(btnRefresh);
        top.add(toolbar, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        model = new DefaultTableModel(new String[]{"Thời gian", "Người dùng", "Hành động", "Đối tượng", "Chi tiết", "Trạng thái", "IP"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        UITheme.styleTable(table);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER));
        add(scroll, BorderLayout.CENTER);

        refreshData();
    }

    @Override
    public void refreshData() {
        SwingWorker<JsonObject, Void> worker = new SwingWorker<>() {
            @Override protected JsonObject doInBackground() { return ApiClient.getInstance().call("LOG_FIND_ALL"); }
            @Override protected void done() {
                try {
                    JsonObject resp = get();
                    if (resp.has("success") && resp.get("success").getAsBoolean()) {
                        model.setRowCount(0);
                        JsonArray arr = resp.getAsJsonArray("data");
                        for (var el : arr) {
                            JsonObject log = el.getAsJsonObject();
                            model.addRow(new Object[]{
                                    str(log, "created_at"),
                                    str(log, "username"),
                                    str(log, "action"),
                                    str(log, "target"),
                                    str(log, "detail"),
                                    str(log, "status"),
                                    str(log, "ip_address")
                            });
                        }
                    } else {
                        JOptionPane.showMessageDialog(LogPanel.this, resp.get("message").getAsString());
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(LogPanel.this, "Lỗi tải dữ liệu: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private String str(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : "";
    }
}