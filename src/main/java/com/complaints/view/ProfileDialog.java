package com.complaints.view;

import com.complaints.controller.ApplicationController;
import com.complaints.model.User;
import com.complaints.util.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

final class ProfileDialog extends JDialog {
    private final PortalFrame frame;
    private final ApplicationController controller;
    private final User user;
    private final JTextField name = new JTextField();
    private final JTextField email = new JTextField();

    ProfileDialog(PortalFrame frame, ApplicationController controller, User user) {
        super(frame, "My profile", ModalityType.APPLICATION_MODAL);
        this.frame = frame; this.controller = controller; this.user = user;
        setSize(440, 350); setLocationRelativeTo(frame);
        setContentPane(build());
    }

    private JPanel build() {
        JPanel root = new JPanel(new BorderLayout(0, 20));
        root.setBorder(new EmptyBorder(24, 26, 22, 26));
        JPanel fields = new JPanel(new GridLayout(0, 1, 0, 8));
        fields.add(UITheme.label("Full name", 12, true, UITheme.TEXT)); name.setText(user.fullName()); fields.add(name);
        fields.add(UITheme.label("Email address", 12, true, UITheme.TEXT)); email.setText(user.email()); fields.add(email);
        fields.add(UITheme.label("Account role", 12, true, UITheme.TEXT));
        fields.add(new JLabel(user.isAdmin() ? "Administrator" : "Customer"));
        root.add(fields, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT)); actions.setOpaque(false);
        JButton password = UITheme.button("Change password", false); password.addActionListener(e -> changePassword());
        JButton cancel = UITheme.button("Cancel", false); cancel.addActionListener(e -> dispose());
        JButton save = UITheme.button("Save profile", true); save.addActionListener(e -> save());
        actions.add(password); actions.add(cancel); actions.add(save);
        root.add(actions, BorderLayout.SOUTH);
        return root;
    }

    private void save() {
        UITheme.run(this, () -> {
            User updated = controller.updateProfile(name.getText(), email.getText());
            SwingUtilities.invokeLater(() -> frame.updateUser(updated));
        }, () -> { JOptionPane.showMessageDialog(this, "Profile updated."); dispose(); });
    }

    private void changePassword() {
        JPasswordField current = new JPasswordField();
        JPasswordField next = new JPasswordField();
        JPasswordField confirm = new JPasswordField();
        JPanel form = new JPanel(new GridLayout(0, 1, 4, 4));
        form.add(new JLabel("Current password")); form.add(current);
        form.add(new JLabel("New password (at least 8 characters)")); form.add(next);
        form.add(new JLabel("Confirm new password")); form.add(confirm);
        if (JOptionPane.showConfirmDialog(this, form, "Change password", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        char[] oldPass = current.getPassword(), newPass = next.getPassword(), confirmed = confirm.getPassword();
        if (!java.util.Arrays.equals(newPass, confirmed)) {
            java.util.Arrays.fill(oldPass, '\0'); java.util.Arrays.fill(newPass, '\0'); java.util.Arrays.fill(confirmed, '\0');
            UITheme.error(this, new IllegalArgumentException("The new passwords do not match.")); return;
        }
        UITheme.run(this, () -> {
            try { controller.changePassword(oldPass, newPass); }
            finally { java.util.Arrays.fill(oldPass, '\0'); java.util.Arrays.fill(newPass, '\0'); java.util.Arrays.fill(confirmed, '\0'); }
        }, () -> JOptionPane.showMessageDialog(this, "Password changed successfully."));
    }
}
