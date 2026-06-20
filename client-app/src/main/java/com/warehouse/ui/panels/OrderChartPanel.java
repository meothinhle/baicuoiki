package com.warehouse.ui.panels;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.warehouse.service.ApiClient;
import com.warehouse.ui.MainFrame;
import com.warehouse.util.UITheme;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;

/**
 * Trang biểu đồ: so sánh số phiếu Nhập kho và Xuất kho.
 * - Biểu đồ cột: số phiếu Nhập vs Xuất theo từng tháng (xu hướng thời gian).
 * - Biểu đồ tròn: tỉ lệ tổng số phiếu Nhập / Xuất, có hiện % trên mỗi lát.
 * Dùng JFreeChart (cần dependency org.jfree:jfreechart trong pom.xml).
 */
public class OrderChartPanel extends JPanel implements MainFrame.RefreshablePanel {

    private JPanel chartContainer;
    // key "yyyy-MM" -> số phiếu nhập / xuất trong tháng đó
    private final Map<String, Integer> importByMonth = new TreeMap<>();
    private final Map<String, Integer> exportByMonth = new TreeMap<>();
    private int totalImport = 0;
    private int totalExport = 0;
    private boolean showPieChart = false;

    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
    };

    public OrderChartPanel() {
        setLayout(new BorderLayout());
        setBackground(UITheme.BG_MAIN);
        setBorder(new EmptyBorder(24, 28, 24, 28));

        // Title và toolbar 2 dòng riêng, tránh đè chồng khi cửa sổ hẹp.
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.add(UITheme.sectionTitleWithIcon("\uD83D\uDCC8", "Biểu đồ nhập - xuất kho"), BorderLayout.WEST);
        top.add(titleRow);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolbar.setOpaque(false);
        toolbar.setBorder(new EmptyBorder(8, 0, 0, 0));

        ButtonGroup chartTypeGroup = new ButtonGroup();
        JToggleButton btnBar = new JToggleButton("Theo tháng (cột)", true);
        JToggleButton btnPie = new JToggleButton("Tỉ lệ tổng (tròn)");
        styleToggle(btnBar);
        styleToggle(btnPie);
        chartTypeGroup.add(btnBar);
        chartTypeGroup.add(btnPie);
        btnBar.addActionListener(e -> { showPieChart = false; renderChart(); });
        btnPie.addActionListener(e -> { showPieChart = true; renderChart(); });
        toolbar.add(btnBar);
        toolbar.add(btnPie);

        JButton btnRefresh = UITheme.grayButton("Tải lại");
        btnRefresh.addActionListener(e -> refreshData());
        toolbar.add(btnRefresh);

        top.add(toolbar);
        add(top, BorderLayout.NORTH);

        chartContainer = new JPanel(new BorderLayout());
        chartContainer.setOpaque(false);
        chartContainer.setBorder(BorderFactory.createLineBorder(UITheme.BORDER));
        add(chartContainer, BorderLayout.CENTER);

        refreshData();
    }

    private void styleToggle(JToggleButton btn) {
        btn.setFont(UITheme.FONT_NORMAL);
        btn.setFocusPainted(false);
        btn.setBackground(Color.WHITE);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1, true),
                new EmptyBorder(6, 14, 6, 14)));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    @Override
    public void refreshData() {
        importByMonth.clear();
        exportByMonth.clear();
        totalImport = 0;
        totalExport = 0;

        SwingWorker<JsonObject[], Void> worker = new SwingWorker<>() {
            @Override
            protected JsonObject[] doInBackground() {
                JsonObject importResp = ApiClient.getInstance().call("IMPORT_FIND_ALL");
                JsonObject exportResp = ApiClient.getInstance().call("EXPORT_FIND_ALL");
                return new JsonObject[]{importResp, exportResp};
            }

            @Override
            protected void done() {
                try {
                    JsonObject[] results = get();
                    countByMonth(results[0], "importDate", importByMonth, true);
                    countByMonth(results[1], "exportDate", exportByMonth, false);
                    renderChart();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(OrderChartPanel.this, "Lỗi tải dữ liệu: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    /** Đếm số phiếu theo tháng (key "yyyy-MM") từ response IMPORT_FIND_ALL/EXPORT_FIND_ALL */
    private void countByMonth(JsonObject resp, String dateField, Map<String, Integer> target, boolean isImport) {
        if (resp == null || !resp.has("success") || !resp.get("success").getAsBoolean()) return;
        JsonArray arr = resp.getAsJsonArray("data");
        if (arr == null) return;
        for (var el : arr) {
            JsonObject o = el.getAsJsonObject();
            if (!o.has(dateField) || o.get(dateField).isJsonNull()) continue;
            String raw = o.get(dateField).getAsString();
            String monthKey = toMonthKey(raw);
            if (monthKey == null) continue;
            target.merge(monthKey, 1, Integer::sum);
            if (isImport) totalImport++; else totalExport++;
        }
    }

    /** Parse chuỗi ngày (thử nhiều format) và trả về key "yyyy-MM", hoặc null nếu không parse được */
    private String toMonthKey(String raw) {
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                LocalDateTime dt = LocalDateTime.parse(raw, fmt);
                return String.format("%04d-%02d", dt.getYear(), dt.getMonthValue());
            } catch (Exception ignored) {}
        }
        // Fallback: nếu chuỗi đã ở dạng "yyyy-MM-dd..." thì cắt thủ công 7 ký tự đầu
        if (raw != null && raw.length() >= 7 && raw.charAt(4) == '-') {
            return raw.substring(0, 7);
        }
        return null;
    }

    private void renderChart() {
        chartContainer.removeAll();
        JFreeChart chart = showPieChart ? buildPieChart() : buildBarChart();
        ChartPanel panel = new ChartPanel(chart);
        panel.setMouseWheelEnabled(true);
        panel.setBackground(Color.WHITE);
        chartContainer.add(panel, BorderLayout.CENTER);
        chartContainer.revalidate();
        chartContainer.repaint();
    }

    private JFreeChart buildBarChart() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        // Hợp nhất danh sách tháng từ cả 2 map để mỗi tháng đều có đủ 2 cột
        // (Nhập = 0 nếu tháng đó không có phiếu nhập, tương tự Xuất).
        var allMonths = new TreeMap<String, Boolean>();
        importByMonth.keySet().forEach(m -> allMonths.put(m, true));
        exportByMonth.keySet().forEach(m -> allMonths.put(m, true));

        for (String month : allMonths.keySet()) {
            dataset.addValue(importByMonth.getOrDefault(month, 0), "Phiếu nhập", month);
            dataset.addValue(exportByMonth.getOrDefault(month, 0), "Phiếu xuất", month);
        }

        JFreeChart chart = ChartFactory.createBarChart(
                null, "Tháng", "Số phiếu", dataset,
                PlotOrientation.VERTICAL, true, true, false);

        chart.setBackgroundPaint(Color.WHITE);
        chart.setTitle(new TextTitle("Số phiếu Nhập kho và Xuất kho theo tháng",
                new Font("Segoe UI", Font.BOLD, 15)));

        BarRenderer renderer = (BarRenderer) chart.getCategoryPlot().getRenderer();
        renderer.setSeriesPaint(0, new Color(0, 121, 107));  // Phiếu nhập - xanh ngọc
        renderer.setSeriesPaint(1, UITheme.DANGER);          // Phiếu xuất - đỏ
        renderer.setShadowVisible(false);
        renderer.setMaximumBarWidth(0.12);
        chart.getCategoryPlot().setBackgroundPaint(Color.WHITE);
        chart.getCategoryPlot().setRangeGridlinePaint(UITheme.BORDER);
        chart.getCategoryPlot().setOutlineVisible(false);

        return chart;
    }

    private JFreeChart buildPieChart() {
        DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
        dataset.setValue("Phiếu nhập", totalImport);
        dataset.setValue("Phiếu xuất", totalExport);

        JFreeChart chart = ChartFactory.createPieChart(null, dataset, true, true, false);
        chart.setBackgroundPaint(Color.WHITE);
        chart.setTitle(new TextTitle("Tỉ lệ tổng số phiếu Nhập / Xuất",
                new Font("Segoe UI", Font.BOLD, 15)));

        PiePlot<String> plot = (PiePlot<String>) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlineVisible(false);
        plot.setLabelFont(new Font("Segoe UI", Font.PLAIN, 13));

        // Hiện cả số lượng và phần trăm trên mỗi lát, ví dụ: "Phiếu nhập: 12 (60%)"
        plot.setLabelGenerator(new StandardPieSectionLabelGenerator(
                "{0}: {1} ({2})",
                new DecimalFormat("0"),
                new DecimalFormat("0.0%")));

        plot.setSectionPaint("Phiếu nhập", new Color(0, 121, 107));
        plot.setSectionPaint("Phiếu xuất", UITheme.DANGER);

        return chart;
    }
}