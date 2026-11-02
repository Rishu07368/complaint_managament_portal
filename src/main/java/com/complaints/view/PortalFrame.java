package com.complaints.view;

import com.complaints.controller.ApplicationController;
import com.complaints.model.User;
import com.complaints.util.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public final class PortalFrame extends JFrame {
    private final ApplicationController controller;
    private User user;
    private final JPanel content = new JPanel(new BorderLayout());
    private final JPanel navigation = new JPanel();
    private final Map<String, JButton> navButtons = new LinkedHashMap<>();
    private final JLabel userName = UITheme.label("", 13, true, UITheme.TEXT);
    private final JLabel roleLabel = UITheme.label("", 11, false, UITheme.MUTED);
    private String currentPage = "Dashboard";

    public PortalFrame(ApplicationController controller, User user) {
        super("Complaint Portal");
        this.controller = controller; this.user = user;
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1080, 700));
        setSize(1320, 850);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        add(sidebar(), BorderLayout.WEST);
        JPanel workspace = new JPanel(new BorderLayout());
        workspace.setBackground(UITheme.BG);
        workspace.add(topbar(), BorderLayout.NORTH);
        content.setBackground(UITheme.BG);
        workspace.add(content, BorderLayout.CENTER);
        add(workspace, BorderLayout.CENTER);
        showPage("Dashboard");
    }

    private JPanel sidebar() {
        JPanel side = new JPanel(new BorderLayout());
        side.setPreferredSize(new Dimension(235, 0));
        side.setBackground(UITheme.NAVY);
        side.setBorder(new EmptyBorder(25, 16, 20, 16));
        JLabel brand = UITheme.label("◈  COMPLAINTS", 16, true, Color.WHITE);
        side.add(brand, BorderLayout.NORTH);
        navigation.setOpaque(false);
        navigation.setLayout(new BoxLayout(navigation, BoxLayout.Y_AXIS));
        navigation.setBorder(new EmptyBorder(35, 0, 0, 0));
        addNav("Dashboard", "⌂");
        addNav("Complaints", "▤");
        if (user.isAdmin()) {
            addNav("Users", "♙");
            addNav("Categories", "◉");
            addNav("Reports", "▥");
        }
        side.add(navigation, BorderLayout.CENTER);
        JPanel footer = new JPanel();
        footer.setOpaque(false);
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        JLabel help = UITheme.label("SERVICE DESK", 10, true, new Color(149, 163, 187));
        JLabel version = UITheme.label("Desktop portal  ·  1.0", 11, false, new Color(149, 163, 187));
        footer.add(help); footer.add(Box.createVerticalStrut(8)); footer.add(version);
        side.add(footer, BorderLayout.SOUTH);
        return side;
    }

    private void addNav(String page, String glyph) {
        JButton button = new JButton(glyph + "   " + page);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setForeground(new Color(196, 205, 222));
        button.setBackground(UITheme.NAVY);
        button.setBorder(new EmptyBorder(12, 14, 12, 12));
        button.setFocusPainted(false);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addActionListener(e -> showPage(page));
        navButtons.put(page, button);
        navigation.add(button); navigation.add(Box.createVerticalStrut(5));
    }

    private JPanel topbar() {
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Color.WHITE);
        top.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0,
                UITheme.BORDER), new EmptyBorder(14, 26, 14, 24)));
        JLabel title = UITheme.label("Complaint Management", 14, true, UITheme.TEXT);
        top.add(title, BorderLayout.WEST);
        JPanel profile = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        profile.setOpaque(false);
        JPanel identity = new JPanel(); identity.setOpaque(false);
        identity.setLayout(new BoxLayout(identity, BoxLayout.Y_AXIS));
        identity.add(userName); identity.add(roleLabel);
        JButton edit = UITheme.button("My profile", false);
        edit.addActionListener(e -> openProfile());
        JButton signOut = UITheme.button("Sign out", false);
        signOut.addActionListener(e -> {
            controller.logout(); dispose(); new LoginFrame(controller).setVisible(true);
        });
        profile.add(identity); profile.add(edit); profile.add(signOut);
        top.add(profile, BorderLayout.EAST);
        refreshIdentity();
        return top;
    }

    private void refreshIdentity() {
        userName.setText(user.fullName());
        roleLabel.setText(user.isAdmin() ? "Administrator" : "Customer account");
    }

    void updateUser(User updated) {
        user = updated;
        refreshIdentity();
    }

    private void showPage(String page) {
        currentPage = page;
        navButtons.forEach((name, button) -> {
            boolean active = name.equals(page);
            button.setBackground(active ? new Color(48, 65, 98) : UITheme.NAVY);
            button.setForeground(active ? Color.WHITE : new Color(196, 205, 222));
        });
        JPanel panel = switch (page) {
            case "Complaints" -> new ComplaintPanel(this, controller, user);
            case "Users" -> new UsersPanel(this, controller, user);
            case "Categories" -> new CategoriesPanel(this, controller, user);
            case "Reports" -> new ReportsPanel(this, controller, user);
            default -> new DashboardPanel(this, controller, user);
        };
        content.removeAll(); content.add(panel, BorderLayout.CENTER);
        content.revalidate(); content.repaint();
    }

    public void refreshCurrentPage() { showPage(currentPage); }
    public void navigate(String page) {
        if (navButtons.containsKey(page)) showPage(page);
    }
    public void openNewComplaint() {
        navigate("Complaints");
        SwingUtilities.invokeLater(() -> {
            if (content.getComponentCount() > 0 && content.getComponent(0) instanceof ComplaintPanel panel) panel.createNew();
        });
    }
    public void openProfile() { new ProfileDialog(this, controller, user).setVisible(true); }
}
