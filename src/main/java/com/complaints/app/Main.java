package com.complaints.app;

import com.complaints.config.*;
import com.complaints.controller.ApplicationController;
import com.complaints.dao.*;
import com.complaints.service.*;
import com.formdev.flatlaf.FlatLightLaf;
import com.complaints.view.LoginFrame;

import javax.swing.*;
import java.awt.*;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        try {
            FlatLightLaf.setup();
            UIManager.put("Component.arc", 12);
            UIManager.put("Button.arc", 12);
            UIManager.put("TextComponent.arc", 10);
            AppConfig config = new AppConfig();
            Database database = new Database(config);
            if (config.getBoolean("db.initialize", true)) SchemaInitializer.initialize(database, config);

            UserDAO users = new UserDAO(database);
            CategoryDAO categories = new CategoryDAO(database);
            categories.seedDefaults();
            char[] adminPassword = config.get("admin.password", "Admin@123").toCharArray();
            users.ensureAdministrator(config.get("admin.name", "Portal Administrator"),
                    config.get("admin.email", "admin@complaints.local"), adminPassword);
            java.util.Arrays.fill(adminPassword, '\0');

            ComplaintDAO complaintDAO = new ComplaintDAO(database);
            ApplicationController controller = new ApplicationController(
                    new AuthenticationService(users),
                    new ComplaintService(complaintDAO, categories, users),
                    new CategoryService(categories),
                    new UserService(users),
                    new ReportService(new ReportDAO(database, complaintDAO)));
            EventQueue.invokeLater(() -> new LoginFrame(controller).setVisible(true));
        } catch (Exception e) {
            EventQueue.invokeLater(() -> {
                JOptionPane.showMessageDialog(null,
                        "The portal could not start.\n\n" + (e.getMessage() == null ? e.toString() : e.getMessage())
                                + "\n\nCheck MySQL availability and application.properties.",
                        "Complaint Portal · Startup error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            });
        }
    }
}
