package com.complaints.view;

import com.complaints.controller.ApplicationController;
import com.complaints.model.User;
import com.complaints.util.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Arrays;

public final class LoginFrame extends JFrame {
    private final ApplicationController controller;
    private final JTextField email = new JTextField();
    private final JPasswordField password = new JPasswordField();
    private final JLabel message = UITheme.label(" ", 12, false, UITheme.RED);
    private final JButton signIn = UITheme.button("Sign in", true);

    public LoginFrame(ApplicationController controller) {
        super("Complaint Portal · Sign in");
        this.controller = controller;
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 600));
        setSize(1080, 700);
        setLocationRelativeTo(null);
        setContentPane(build());
        getRootPane().setDefaultButton(signIn);
    }

    private JPanel build() {
        JPanel root = new JPanel(new GridLayout(1, 2));
        JPanel brand = new JPanel(new GridBagLayout());
        brand.setBackground(UITheme.NAVY);
        JPanel brandContent = new JPanel();
        brandContent.setOpaque(false);
        brandContent.setLayout(new BoxLayout(brandContent, BoxLayout.Y_AXIS));
        JLabel mark = UITheme.label("CP  /  SERVICE DESK", 15, true, new Color(174, 193, 255));
        JLabel headline = UITheme.label("<html>Every concern,<br>moving forward.</html>", 38, true, Color.WHITE);
        JLabel copy = UITheme.label("<html>A clear, accountable place to raise concerns,<br>follow progress, and make better service happen.</html>",
                15, false, new Color(202, 211, 227));
        brandContent.add(mark);
        brandContent.add(Box.createVerticalStrut(54));
        brandContent.add(headline);
        brandContent.add(Box.createVerticalStrut(18));
        brandContent.add(copy);
        brand.add(brandContent);

        JPanel formArea = new JPanel(new GridBagLayout());
        formArea.setBackground(UITheme.BG);
        JPanel form = new JPanel();
        form.setBackground(UITheme.BG);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(new EmptyBorder(35, 44, 35, 44));
        JLabel title = UITheme.label("Welcome back", 28, true, UITheme.TEXT);
        JLabel subtitle = UITheme.label("Sign in to your complaint portal", 14, false, UITheme.MUTED);
        form.add(title); form.add(Box.createVerticalStrut(8)); form.add(subtitle);
        form.add(Box.createVerticalStrut(34));
        form.add(fieldLabel("Email address")); email.setPreferredSize(new Dimension(340, 42)); email.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        form.add(email); form.add(Box.createVerticalStrut(19));
        form.add(fieldLabel("Password"));
        password.setPreferredSize(new Dimension(340, 42)); password.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        form.add(password);
        JCheckBox show = new JCheckBox("Show password");
        show.setOpaque(false); show.setForeground(UITheme.MUTED);
        char echoChar = password.getEchoChar();
        show.addActionListener(e -> password.setEchoChar(show.isSelected() ? (char) 0 : echoChar));
        form.add(Box.createVerticalStrut(8)); form.add(show);
        form.add(Box.createVerticalStrut(8)); form.add(message);
        signIn.setAlignmentX(Component.LEFT_ALIGNMENT);
        signIn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        signIn.addActionListener(e -> login());
        form.add(Box.createVerticalStrut(12)); form.add(signIn);
        JButton register = UITheme.button("Create an account", false);
        register.setAlignmentX(Component.LEFT_ALIGNMENT);
        register.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        register.addActionListener(e -> register());
        form.add(Box.createVerticalStrut(12)); form.add(register);
        formArea.add(form);
        root.add(brand); root.add(formArea);
        return root;
    }

    private JLabel fieldLabel(String text) {
        JLabel label = UITheme.label(text, 13, true, UITheme.TEXT);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private void login() {
        char[] secret = password.getPassword();
        signIn.setEnabled(false); signIn.setText("Signing in…"); message.setText(" ");
        UITheme.load(this, () -> {
            try { return controller.login(email.getText(), secret); }
            finally { Arrays.fill(secret, '\0'); }
        }, user -> {
            dispose();
            new PortalFrame(controller, user).setVisible(true);
        }, error -> {
            UITheme.error(this, error);
            signIn.setEnabled(true); signIn.setText("Sign in");
        });
    }

    private void register() {
        JTextField name = new JTextField();
        JTextField newEmail = new JTextField();
        JPasswordField newPassword = new JPasswordField();
        JPasswordField confirmation = new JPasswordField();
        JPanel fields = new JPanel(new GridLayout(0, 1, 5, 5));
        fields.add(new JLabel("Full name")); fields.add(name);
        fields.add(new JLabel("Email address")); fields.add(newEmail);
        fields.add(new JLabel("Password (at least 8 characters)")); fields.add(newPassword);
        fields.add(new JLabel("Confirm password")); fields.add(confirmation);
        if (JOptionPane.showConfirmDialog(this, fields, "Create your account", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        char[] secret = newPassword.getPassword();
        char[] confirm = confirmation.getPassword();
        if (!Arrays.equals(secret, confirm)) {
            Arrays.fill(secret, '\0'); Arrays.fill(confirm, '\0');
            UITheme.error(this, new IllegalArgumentException("The passwords do not match."));
            return;
        }
        UITheme.load(this, () -> {
            try { return controller.register(name.getText(), newEmail.getText(), secret); }
            finally { Arrays.fill(secret, '\0'); Arrays.fill(confirm, '\0'); }
        }, ignored -> JOptionPane.showMessageDialog(this, "Your account has been created. You can now sign in.",
                "Registration complete", JOptionPane.INFORMATION_MESSAGE));
    }
}
