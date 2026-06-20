package com.warehouse.ui.panels;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.warehouse.service.ApiClient;
import com.warehouse.ui.MainFrame;
import com.warehouse.util.UITheme;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Trang biểu đồ: số lượng sản phẩm theo danh mục.
 * Dùng JFreeChart (cần dependency org.jfree:jfreechart trong pom.xml).
 */
public class ProductChartPanel extends JPanel implements MainFrame.RefreshablePanel {

    private JPanel chartContainer;
    private Map<String, Integer> dataByCategory = new LinkedHashMap<>();
    private boolean showPieChart = false;

    // Bảng màu nhất quán với UITheme cho từng danh mục, lặp lại nếu nhiều hơn
    private static final Color[] PALETTE = {
            UITheme.PRIMARY, UITheme.SUCCESS, UITheme.WARNING, UITheme.DANGER,
            new Color(123, 31, 162), new Color(0, 121, 107), new Color(255, 112, 67),
            new Color(57, 73, 171), new Color(216, 27, 96), new Color(0, 151, 167)
    };

    public ProductChartPanel() {
        setLayout(new BorderLayout());
        setBackground(UITheme.BG_MAIN);
        setBorder(new EmptyBorder(24, 28, 24, 28));

        // Title và toolbar 2 dòng riêng, tránh đè chồng khi cửa sổ hẹp
        // (cùng pattern với ProductPanel/CategoryPanel...).
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.add(UITheme.sectionTitleWithIcon("\uD83D\uDCC8", "Biểu đồ sản phẩm"), BorderLayout.WEST);
        top.add(titleRow);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolbar.setOpaque(false);
        toolbar.setBorder(new EmptyBorder(8, 0, 0, 0));

        ButtonGroup chartTypeGroup = new ButtonGroup();
        JToggleButton btnBar = new JToggleButton("Biểu đồ cột", true);
        JToggleButton btnPie = new JToggleButton("Biểu đồ tròn");
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
        SwingWorker<JsonObject, Void> worker = new SwingWorker<>() {
            @Override
            protected JsonObject doInBackground() {
                return ApiClient.getInstance().call("PRODUCT_FIND_ALL");
            }

            @Override
            protected void done() {
                try {
                    JsonObject resp = get();
                    if (resp.has("success") && resp.get("success").getAsBoolean()) {
                        dataByCategory.clear();
                        JsonArray arr = resp.getAsJsonArray("data");
                        for (var el : arr) {
                            JsonObject p = el.getAsJsonObject();
                            String cat = p.has("categoryName") && !p.get("categoryName").isJsonNull()
                                    ? p.get("categoryName").getAsString() : "Khác";
                            dataByCategory.merge(cat, 1, Integer::sum);
                        }
                        renderChart();
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ProductChartPanel.this, "Lỗi tải dữ liệu: " + ex.getMessage());
                }
            }
        };
        worker.execute();
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
        dataByCategory.forEach((cat, qty) -> dataset.addValue(qty, "Số lượng sản phẩm", cat));

        JFreeChart chart = ChartFactory.createBarChart(
                null, "Danh mục", "Số lượng sản phẩm", dataset,
                PlotOrientation.VERTICAL, false, true, false);

        chart.setBackgroundPaint(Color.WHITE);
        chart.setTitle(new TextTitle("Số lượng sản phẩm theo danh mục",
                new Font("Segoe UI", Font.BOLD, 15)));

        BarRenderer renderer = (BarRenderer) chart.getCategoryPlot().getRenderer();
        renderer.setSeriesPaint(0, UITheme.PRIMARY);
        renderer.setShadowVisible(false);
        renderer.setMaximumBarWidth(0.12);
        chart.getCategoryPlot().setBackgroundPaint(Color.WHITE);
        chart.getCategoryPlot().setRangeGridlinePaint(UITheme.BORDER);
        chart.getCategoryPlot().setOutlineVisible(false);

        return chart;
    }

    private JFreeChart buildPieChart() {
        DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
        dataByCategory.forEach(dataset::setValue);

        JFreeChart chart = ChartFactory.createPieChart(null, dataset, true, true, false);
        chart.setBackgroundPaint(Color.WHITE);
        chart.setTitle(new TextTitle("Tỉ lệ sản phẩm theo danh mục",
                new Font("Segoe UI", Font.BOLD, 15)));

        PiePlot<String> plot = (PiePlot<String>) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlineVisible(false);
        plot.setLabelFont(new Font("Segoe UI", Font.PLAIN, 12));
        plot.setLabelGenerator(null); // ẩn label trên từng lát, chỉ dùng legend cho gọn

        int i = 0;
        for (String key : dataByCategory.keySet()) {
            plot.setSectionPaint(key, PALETTE[i % PALETTE.length]);
            i++;
        }

        return chart;
    }
}