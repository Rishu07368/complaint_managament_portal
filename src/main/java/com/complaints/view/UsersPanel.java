package com.complaints.view;

import com.complaints.controller.ApplicationController;
import com.complaints.model.User;
import com.complaints.util.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

final class UsersPanel extends JPanel {
    private final PortalFrame frame;
    private final ApplicationController controller;
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Name", "Email", "Role", "Account status"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = UITheme.table(new JTable(model));

    UsersPanel(PortalFrame frame, ApplicationController controller, User user) {
        this.frame = frame; this.controller = controller;
        setLayout(new BorderLayout(0, 18)); setBackground(UITheme.BG); setBorder(new EmptyBorder(26, 28, 26, 28));
        JPanel header = new JPanel(new BorderLayout()); header.setOpaque(false);
        JPanel copy = new JPanel(); copy.setOpaque(false); copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        copy.add(UITheme.label("People & access", 24, true, UITheme.TEXT));
        copy.add(Box.createVerticalStrut(5));
        copy.add(UITheme.label("Registered portal users and account roles.", 13, false, UITheme.MUTED));
        header.add(copy, BorderLayout.WEST);
        JButton profile = UITheme.button("Edit my profile", false); profile.addActionListener(e -> frame.openProfile());
        header.add(profile, BorderLayout.EAST); add(header, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT)); footer.setOpaque(false);
        JButton refresh = UITheme.button("Refresh users", false); refresh.addActionListener(e -> load());
        footer.add(refresh); add(footer, BorderLayout.SOUTH);
        load();
    }

    private void load() {
        UITheme.load(this, controller::users, result -> {
            @SuppressWarnings("unchecked") List<User> users = (List<User>) result;
            model.setRowCount(0);
            for (User user : users) model.addRow(new Object[]{user.fullName(), user.email(),
                    user.isAdmin() ? "Administrator" : "User", user.active() ? "Active" : "Inactive"});
        });
    }
}
