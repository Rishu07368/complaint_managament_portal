package com.complaints.view;

import com.complaints.controller.ApplicationController;
import com.complaints.model.*;
import com.complaints.util.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

final class DashboardPanel extends JPanel {
    private final PortalFrame frame;
    private final ApplicationController controller;
    private final User user;
    private final JPanel body = new JPanel(new BorderLayout(18, 18));
    private DashboardData data;
    private JTable recentTable;
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"Complaint ID", "Title", "Category", "Priority", "Status", "Updated"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };

    DashboardPanel(PortalFrame frame, ApplicationController controller, User user) {
        this.frame = frame; this.controller = controller; this.user = user;
        setLayout(new BorderLayout());
        setBackground(UITheme.BG);
        setBorder(new EmptyBorder(24, 28, 26, 28));
        body.setOpaque(false);
        add(body, BorderLayout.CENTER);
        UITheme.load(this, controller::dashboard, result -> {
            data = (DashboardData) result;
            render();
        });
        renderLoading();
    }

    private void renderLoading() {
        body.add(UITheme.label("Loading your dashboard…", 15, false, UITheme.MUTED), BorderLayout.NORTH);
    }

    private void render() {
        body.removeAll();
        JPanel layout = new JPanel(new BorderLayout(18, 18));
        layout.setOpaque(false);
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JPanel heading = new JPanel(); heading.setOpaque(false); heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        heading.add(UITheme.label("Good day, " + user.fullName().split(" ")[0], 24, true, UITheme.TEXT));
        heading.add(Box.createVerticalStrut(5));
        heading.add(UITheme.label("Here’s the latest view of your service requests.", 13, false, UITheme.MUTED));
        header.add(heading, BorderLayout.WEST);
        JButton newComplaint = UITheme.button("+  New complaint", true);
        newComplaint.addActionListener(e -> frame.openNewComplaint());
        header.add(newComplaint, BorderLayout.EAST);
        layout.add(header, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(16, 16));
        center.setOpaque(false);
        JPanel cards = new JPanel(new GridLayout(1, 5, 12, 12));
        cards.setOpaque(false);
        cards.add(metric("Total complaints", "TOTAL", new Color(72, 101, 242)));
        cards.add(metric("Pending", "PENDING", UITheme.AMBER));
        cards.add(metric("In progress", "IN_PROGRESS", UITheme.PRIMARY));
        cards.add(metric("Resolved", "RESOLVED", UITheme.GREEN));
        cards.add(metric("Rejected", "REJECTED", UITheme.RED));
        center.add(cards, BorderLayout.NORTH);

        JPanel charts = new JPanel(new GridLayout(1, 2, 16, 16)); charts.setOpaque(false);
        BarChartPanel statusChart = new BarChartPanel(false); statusChart.setData(data.statuses());
        charts.add(chartCard("Status distribution", statusChart));
        BarChartPanel categoryChart = new BarChartPanel(true); categoryChart.setData(data.categories());
        charts.add(chartCard("Complaints by category", categoryChart));
        center.add(charts, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new GridLayout(1, 2, 16, 16)); bottom.setOpaque(false);
        bottom.add(recentCard());
        bottom.add(activityCard());
        center.add(bottom, BorderLayout.SOUTH);
        layout.add(center, BorderLayout.CENTER);
        body.add(layout, BorderLayout.CENTER);
        body.revalidate(); body.repaint();
    }

    private JPanel metric(String label, String key, Color accent) {
        JPanel card = UITheme.card(); card.setLayout(new BorderLayout(0, 12));
        JPanel stripe = new JPanel(); stripe.setBackground(accent); stripe.setPreferredSize(new Dimension(4, 4));
        card.add(stripe, BorderLayout.NORTH);
        card.add(UITheme.label(label, 12, false, UITheme.MUTED), BorderLayout.CENTER);
        card.add(UITheme.label(Long.toString(data.counts().getOrDefault(key, 0L)), 28, true, UITheme.TEXT), BorderLayout.SOUTH);
        return card;
    }

    private JPanel chartCard(String title, JComponent chart) {
        JPanel card = UITheme.card(); card.setLayout(new BorderLayout(0, 12));
        card.add(UITheme.label(title, 14, true, UITheme.TEXT), BorderLayout.NORTH);
        card.add(chart, BorderLayout.CENTER);
        return card;
    }

    private JPanel recentCard() {
        JPanel card = UITheme.card(); card.setLayout(new BorderLayout(0, 10));
        card.add(UITheme.label("Recent complaints", 14, true, UITheme.TEXT), BorderLayout.NORTH);
        recentTable = UITheme.table(new JTable(tableModel));
        List<Complaint> rows = data.recent().stream().limit(5).toList();
        for (Complaint complaint : rows) tableModel.addRow(new Object[]{complaint.code(), complaint.title(),
                complaint.category(), complaint.priority(), complaint.status().replace('_', ' '),
                complaint.updatedAt().format(DateTimeFormatter.ofPattern("MMM d, HH:mm"))});
        card.add(new JScrollPane(recentTable), BorderLayout.CENTER);
        JButton view = UITheme.button("View selected complaint", false);
        view.addActionListener(e -> {
            int row = recentTable.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(this, "Select a complaint first."); return; }
            Complaint complaint = rows.get(recentTable.convertRowIndexToModel(row));
            new ComplaintDialog(frame, controller, user, complaint, true).setVisible(true);
        });
        card.add(view, BorderLayout.SOUTH);
        return card;
    }

    private JPanel activityCard() {
        JPanel card = UITheme.card(); card.setLayout(new BorderLayout(0, 10));
        card.add(UITheme.label("Recent activity", 14, true, UITheme.TEXT), BorderLayout.NORTH);
        JPanel list = new JPanel(); list.setOpaque(false); list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        if (data.activity().isEmpty()) {
            list.add(UITheme.label("No recent activity to show.", 13, false, UITheme.MUTED));
        } else {
            for (String line : data.activity()) {
                JLabel item = UITheme.label("•  " + line, 12, false, UITheme.TEXT);
                item.setBorder(new EmptyBorder(8, 0, 8, 0)); list.add(item);
            }
        }
        card.add(new JScrollPane(list), BorderLayout.CENTER);
        return card;
    }
}
