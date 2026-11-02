package com.complaints.util;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.function.Consumer;

public final class UITheme {
    public static final Color NAVY = new Color(20, 32, 54);
    public static final Color PRIMARY = new Color(72, 101, 242);
    public static final Color TEXT = new Color(35, 45, 64);
    public static final Color MUTED = new Color(117, 129, 151);
    public static final Color BG = new Color(246, 248, 252);
    public static final Color BORDER = new Color(226, 231, 240);
    public static final Color GREEN = new Color(29, 151, 111);
    public static final Color AMBER = new Color(213, 139, 34);
    public static final Color RED = new Color(205, 75, 85);

    private UITheme() {}

    public static JButton button(String text, boolean primary) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setForeground(primary ? Color.WHITE : TEXT);
        button.setBackground(primary ? PRIMARY : Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(primary ? PRIMARY : BORDER, 1, true),
                new EmptyBorder(9, 15, 9, 15)));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    public static JLabel label(String text, int size, boolean bold, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", bold ? Font.BOLD : Font.PLAIN, size));
        label.setForeground(color);
        return label;
    }

    public static JPanel card() {
        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true), new EmptyBorder(18, 18, 18, 18)));
        return panel;
    }

    @FunctionalInterface
    public interface CheckedSupplier<T> { T get() throws Exception; }

    @FunctionalInterface
    public interface CheckedRunnable { void run() throws Exception; }

    public static <T> void load(Component parent, CheckedSupplier<T> task, Consumer<T> success) {
        load(parent, task, success, error -> error(parent, error));
    }

    public static <T> void load(Component parent, CheckedSupplier<T> task, Consumer<T> success,
                                 Consumer<Throwable> failure) {
        new SwingWorker<T, Void>() {
            @Override protected T doInBackground() throws Exception { return task.get(); }
            @Override protected void done() {
                try { success.accept(get()); }
                catch (Exception e) {
                    Throwable cause = e.getCause() == null ? e : e.getCause();
                    failure.accept(cause);
                }
            }
        }.execute();
    }

    public static void run(Component parent, CheckedRunnable task, Runnable success) {
        load(parent, () -> { task.run(); return null; }, ignored -> success.run());
    }

    public static void run(Component parent, CheckedRunnable task, Runnable success, Consumer<Throwable> failure) {
        load(parent, () -> { task.run(); return null; }, ignored -> success.run(), failure);
    }

    public static void error(Component parent, Throwable error) {
        String message = error.getMessage();
        if (message == null || message.isBlank()) message = error.getClass().getSimpleName();
        JOptionPane.showMessageDialog(parent, message, "Unable to complete action", JOptionPane.ERROR_MESSAGE);
    }

    public static JTable table(JTable table) {
        table.setRowHeight(38);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setForeground(TEXT);
        table.setGridColor(BORDER);
        table.setSelectionBackground(new Color(231, 237, 255));
        table.setSelectionForeground(TEXT);
        table.setShowVerticalLines(false);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(248, 249, 252));
        table.getTableHeader().setForeground(MUTED);
        table.getTableHeader().setPreferredSize(new Dimension(0, 40));
        return table;
    }
}
