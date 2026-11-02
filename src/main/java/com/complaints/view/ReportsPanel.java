package com.complaints.view;

import com.complaints.controller.ApplicationController;
import com.complaints.model.ReportData;
import com.complaints.model.User;
import com.complaints.util.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

final class ReportsPanel extends JPanel {
    private final ApplicationController controller;
    private final JPanel content = new JPanel(new BorderLayout(16, 16));
    private ReportData currentReport;

    ReportsPanel(PortalFrame frame, ApplicationController controller, User user) {
        this.controller = controller;
        setLayout(new BorderLayout()); setBackground(UITheme.BG); setBorder(new EmptyBorder(26, 28, 26, 28));
        content.setOpaque(false); add(content, BorderLayout.CENTER);
        content.add(UITheme.label("Loading database analytics…", 14, false, UITheme.MUTED), BorderLayout.NORTH);
        load();
    }

    private void load() {
        UITheme.load(this, controller::report, result -> render((ReportData) result));
    }

    private void render(ReportData report) {
        currentReport = report;
        content.removeAll();
        JPanel layout = new JPanel(new BorderLayout(16, 16)); layout.setOpaque(false);
        JPanel header = new JPanel(new BorderLayout()); header.setOpaque(false);
        JPanel copy = new JPanel(); copy.setOpaque(false); copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        copy.add(UITheme.label("Reports & analytics", 24, true, UITheme.TEXT));
        copy.add(Box.createVerticalStrut(5));
        copy.add(UITheme.label("Performance indicators calculated from live complaint records.", 13, false, UITheme.MUTED));
        header.add(copy, BorderLayout.WEST);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0)); actions.setOpaque(false);
        JButton export = UITheme.button("Export CSV", false); export.addActionListener(e -> exportCsv());
        JButton refresh = UITheme.button("Refresh reports", false); refresh.addActionListener(e -> load());
        actions.add(export); actions.add(refresh);
        header.add(actions, BorderLayout.EAST); layout.add(header, BorderLayout.NORTH);

        JPanel stats = new JPanel(new GridLayout(1, 3, 12, 12)); stats.setOpaque(false);
        stats.add(metric("Resolution rate", String.format("%.1f%%", report.resolutionRate())));
        stats.add(metric("Average resolution", report.averageResolutionHours() == 0 ? "—"
                : String.format("%.1f hours", report.averageResolutionHours())));
        stats.add(metric("Resolved complaints", Long.toString(report.statuses().getOrDefault("RESOLVED", 0L))));
        JPanel chartGrid = new JPanel(new GridLayout(2, 2, 14, 14)); chartGrid.setOpaque(false);
        Map<String, Long> monthData = new LinkedHashMap<>();
        for (ReportData.MonthlyCount entry : report.monthly()) monthData.put(entry.month(), entry.count());
        chartGrid.add(chartCard("Complaints by category", report.categories(), true));
        chartGrid.add(chartCard("Priority distribution", report.priorities(), true));
        chartGrid.add(chartCard("Status overview", report.statuses(), false));
        chartGrid.add(chartCard("Monthly complaint volume", monthData, false));
        JPanel center = new JPanel(new BorderLayout(14, 14)); center.setOpaque(false);
        center.add(stats, BorderLayout.NORTH); center.add(chartGrid, BorderLayout.CENTER);
        layout.add(center, BorderLayout.CENTER);
        content.add(layout, BorderLayout.CENTER); content.revalidate(); content.repaint();
    }

    private void exportCsv() {
        if (currentReport == null) return;
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export complaint analytics");
        chooser.setSelectedFile(new java.io.File("complaint-report.csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            StringBuilder csv = new StringBuilder("Metric,Value\n");
            appendMetric(csv, "Resolution rate (%)", String.format(java.util.Locale.ROOT, "%.2f", currentReport.resolutionRate()));
            appendMetric(csv, "Average resolution hours", String.format(java.util.Locale.ROOT, "%.2f", currentReport.averageResolutionHours()));
            appendGroup(csv, "Category", currentReport.categories());
            appendGroup(csv, "Priority", currentReport.priorities());
            appendGroup(csv, "Status", currentReport.statuses());
            for (ReportData.MonthlyCount entry : currentReport.monthly())
                appendMetric(csv, "Month " + entry.month(), Long.toString(entry.count()));
            Files.writeString(chooser.getSelectedFile().toPath(), csv.toString(), StandardCharsets.UTF_8);
            JOptionPane.showMessageDialog(this, "Report exported successfully.", "Export complete", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            UITheme.error(this, e);
        }
    }

    private static void appendGroup(StringBuilder csv, String group, Map<String, Long> values) {
        for (Map.Entry<String, Long> entry : values.entrySet())
            appendMetric(csv, group + " · " + entry.getKey(), Long.toString(entry.getValue()));
    }

    private static void appendMetric(StringBuilder csv, String name, String value) {
        csv.append('"').append(name.replace("\"", "\"\"")).append("\",\"")
                .append(value.replace("\"", "\"\"")).append("\"\n");
    }

    private JPanel metric(String title, String value) {
        JPanel card = UITheme.card(); card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.add(UITheme.label(title, 12, false, UITheme.MUTED)); card.add(Box.createVerticalStrut(10));
        card.add(UITheme.label(value, 24, true, UITheme.TEXT));
        return card;
    }

    private JPanel chartCard(String title, Map<String, Long> data, boolean horizontal) {
        JPanel card = UITheme.card(); card.setLayout(new BorderLayout(0, 10));
        card.add(UITheme.label(title, 14, true, UITheme.TEXT), BorderLayout.NORTH);
        BarChartPanel chart = new BarChartPanel(horizontal); chart.setData(data);
        card.add(chart, BorderLayout.CENTER);
        return card;
    }
}
