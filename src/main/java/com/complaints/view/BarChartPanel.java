package com.complaints.view;

import com.complaints.util.UITheme;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

final class BarChartPanel extends JPanel {
    private Map<String, Long> data = new LinkedHashMap<>();
    private final boolean horizontal;

    BarChartPanel(boolean horizontal) {
        this.horizontal = horizontal;
        setOpaque(false);
        setPreferredSize(new Dimension(320, 220));
    }

    void setData(Map<String, Long> data) {
        this.data = data == null ? new LinkedHashMap<>() : new LinkedHashMap<>(data);
        repaint();
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int width = getWidth(), height = getHeight();
        if (data.isEmpty()) {
            g.setColor(UITheme.MUTED);
            g.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            g.drawString("No data available yet", 16, 30);
            g.dispose();
            return;
        }
        long max = Math.max(1L, data.values().stream().mapToLong(Long::longValue).max().orElse(1));
        Color[] colors = {UITheme.PRIMARY, UITheme.GREEN, UITheme.AMBER, new Color(138, 104, 214), UITheme.RED};
        int index = 0;
        if (horizontal) {
            int row = Math.max(31, (height - 8) / data.size());
            for (Map.Entry<String, Long> entry : data.entrySet()) {
                int y = index * row + 4;
                g.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                g.setColor(UITheme.TEXT);
                String name = entry.getKey().replace('_', ' ');
                g.drawString(name.length() > 19 ? name.substring(0, 17) + "…" : name, 0, y + 14);
                int x = 132;
                int available = Math.max(30, width - x - 35);
                g.setColor(new Color(238, 241, 247));
                g.fillRoundRect(x, y + 2, available, 13, 8, 8);
                g.setColor(colors[index++ % colors.length]);
                int bar = (int) Math.max(entry.getValue() == 0 ? 0 : 6,
                        available * (double) entry.getValue() / max);
                g.fillRoundRect(x, y + 2, bar, 13, 8, 8);
                g.setColor(UITheme.MUTED);
                g.drawString(Long.toString(entry.getValue()), x + available + 7, y + 14);
            }
        } else {
            int bottom = height - 31, graphHeight = Math.max(40, height - 64);
            int slot = Math.max(26, (width - 24) / data.size());
            for (Map.Entry<String, Long> entry : data.entrySet()) {
                int x = 12 + index * slot;
                int barHeight = (int) Math.max(entry.getValue() == 0 ? 0 : 5,
                        graphHeight * (double) entry.getValue() / max);
                g.setColor(colors[index % colors.length]);
                g.fillRoundRect(x + 4, bottom - barHeight, Math.max(12, slot - 13), barHeight, 8, 8);
                g.setColor(UITheme.TEXT); g.setFont(new Font("Segoe UI", Font.BOLD, 11));
                g.drawString(Long.toString(entry.getValue()), x + 5, bottom - barHeight - 5);
                g.setColor(UITheme.MUTED); g.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                String label = entry.getKey().length() > 10 ? entry.getKey().substring(0, 9) + "…" : entry.getKey();
                g.drawString(label, x + 2, height - 9);
                index++;
            }
        }
        g.dispose();
    }
}
