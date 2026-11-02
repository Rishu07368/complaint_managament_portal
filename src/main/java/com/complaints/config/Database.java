package com.complaints.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Database {
    private static final Pattern DATABASE_PATH = Pattern.compile("^(jdbc:mysql://[^/]+)(?:/[^?]*)?(\\?.*)?$");
    private final AppConfig config;

    public Database(AppConfig config) {
        this.config = config;
    }

    public Connection connect() throws SQLException {
        String configuredUrl = config.get("db.url", "jdbc:mysql://localhost:3306/complaint_portal?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        Matcher matcher = DATABASE_PATH.matcher(configuredUrl);
        if (!matcher.matches()) throw new IllegalArgumentException("db.url must be a MySQL JDBC URL.");
        String url = matcher.group(1) + "/" + databaseName() + (matcher.group(2) == null ? "" : matcher.group(2));
        return DriverManager.getConnection(
                url,
                config.get("db.username", "root"),
                config.get("db.password", ""));
    }

    public String databaseName() {
        String name = config.get("db.name", "complaint_portal");
        if (!name.matches("[A-Za-z0-9_]+")) throw new IllegalArgumentException("db.name may contain only letters, numbers, and underscores.");
        return name;
    }
}
