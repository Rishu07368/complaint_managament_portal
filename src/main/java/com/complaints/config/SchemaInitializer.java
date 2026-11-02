package com.complaints.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SchemaInitializer {
    private static final Pattern SERVER_URL = Pattern.compile("^(jdbc:mysql://[^/]+)(?:/[^?]*)?(\\?.*)?$");
    private static final Map<String, Map<String, String>> FOREIGN_KEY_COLUMNS = Map.of(
            "complaints", Map.of(
                    "user_id", "users.id",
                    "category_id", "complaint_categories.id",
                    "assigned_admin_id", "users.id"),
            "complaint_updates", Map.of(
                    "complaint_id", "complaints.id",
                    "actor_id", "users.id"),
            "complaint_assignments", Map.of(
                    "complaint_id", "complaints.id",
                    "admin_id", "users.id",
                    "assigned_by", "users.id"),
            "activity_logs", Map.of(
                    "user_id", "users.id",
                    "complaint_id", "complaints.id"));

    private SchemaInitializer() {}

    public static void initialize(Database database, AppConfig config) throws SQLException, IOException {
        Matcher matcher = SERVER_URL.matcher(config.get("db.url",
                "jdbc:mysql://localhost:3306/complaint_portal?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"));
        if (!matcher.matches()) throw new IllegalArgumentException("db.url must be a MySQL JDBC URL.");
        String serverUrl = matcher.group(1) + (matcher.group(2) == null ? "" : matcher.group(2));
        try (Connection connection = DriverManager.getConnection(serverUrl,
                config.get("db.username", "root"), config.get("db.password", ""));
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE IF NOT EXISTS `" + database.databaseName() + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci");
        }

        String schema;
        try (var input = SchemaInitializer.class.getClassLoader().getResourceAsStream("database/schema.sql")) {
            if (input == null) throw new IOException("Missing database/schema.sql from application resources.");
            schema = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        try (Connection connection = database.connect(); Statement statement = connection.createStatement()) {
            for (String sql : schema.split(";")) {
                String command = sql.lines()
                        .filter(line -> !line.stripLeading().startsWith("--"))
                        .reduce("", (left, right) -> left + " " + right).trim();
                if (!command.isBlank() && !command.toUpperCase().startsWith("CREATE DATABASE")
                        && !command.toUpperCase().startsWith("USE ")) {
                    statement.execute(adaptForeignKeyColumnTypes(connection, database.databaseName(), command));
                }
            }
            migrateLegacyUsers(connection, database.databaseName());
            migrateLegacyCategories(connection, database.databaseName());
            migrateLegacyComplaints(connection, database.databaseName());
        }
    }

    private static void migrateLegacyUsers(Connection connection, String databaseName) throws SQLException {
        addColumns(connection, databaseName, "users", List.of(
                new ColumnDefinition("role", "ENUM('ADMIN','USER') NOT NULL DEFAULT 'USER'"),
                new ColumnDefinition("active", "BOOLEAN NOT NULL DEFAULT TRUE"),
                new ColumnDefinition("created_at", "TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP"),
                new ColumnDefinition("updated_at", "TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
        ));
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE users SET created_at = CURRENT_TIMESTAMP WHERE created_at IS NULL");
            statement.executeUpdate("UPDATE users SET updated_at = CURRENT_TIMESTAMP WHERE updated_at IS NULL");
            statement.execute("ALTER TABLE users MODIFY full_name VARCHAR(120) NOT NULL");
            statement.execute("ALTER TABLE users MODIFY email VARCHAR(190) NOT NULL");
            statement.execute("ALTER TABLE users MODIFY created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");
            statement.execute("ALTER TABLE users MODIFY updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
        }
    }

    private static void migrateLegacyCategories(Connection connection, String databaseName) throws SQLException {
        addColumns(connection, databaseName, "complaint_categories", List.of(
                new ColumnDefinition("active", "BOOLEAN NOT NULL DEFAULT TRUE"),
                new ColumnDefinition("created_at", "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP")
        ));
    }

    private static void migrateLegacyComplaints(Connection connection, String databaseName) throws SQLException {
        String userIdType = columnType(connection, databaseName, "users", "id");
        String categoryIdType = columnType(connection, databaseName, "complaint_categories", "id");
        if (userIdType == null || categoryIdType == null) {
            throw new SQLException("Cannot upgrade complaints: users.id or complaint_categories.id is missing.");
        }
        addColumns(connection, databaseName, "complaints", List.of(
                new ColumnDefinition("complaint_code", "VARCHAR(32) NULL"),
                new ColumnDefinition("category_id", categoryIdType + " NULL"),
                new ColumnDefinition("title", "VARCHAR(180) NULL"),
                new ColumnDefinition("description", "TEXT NULL"),
                new ColumnDefinition("priority", "VARCHAR(24) NULL"),
                new ColumnDefinition("status", "VARCHAR(24) NULL"),
                new ColumnDefinition("assigned_admin_id", userIdType + " NULL"),
                new ColumnDefinition("admin_remarks", "TEXT NULL"),
                new ColumnDefinition("created_at", "TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP"),
                new ColumnDefinition("updated_at", "TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"),
                new ColumnDefinition("resolved_at", "TIMESTAMP NULL")
        ));

        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE complaints MODIFY priority VARCHAR(24) NULL");
            statement.execute("ALTER TABLE complaints MODIFY status VARCHAR(24) NULL");
        }

        try (Statement statement = connection.createStatement()) {
            if (columnExists(connection, databaseName, "complaints", "subject")) {
                statement.executeUpdate("UPDATE complaints SET title = LEFT(subject, 180) WHERE title IS NULL OR TRIM(title) = ''");
            }
            if (columnExists(connection, databaseName, "complaints", "category")) {
                statement.executeUpdate("""
                        INSERT IGNORE INTO complaint_categories (name, description)
                        SELECT DISTINCT LEFT(COALESCE(NULLIF(TRIM(category), ''), 'Other'), 80),
                               'Migrated from existing complaints'
                        FROM complaints
                        """);
            }
            statement.executeUpdate("""
                    INSERT IGNORE INTO complaint_categories (name, description)
                    VALUES ('Other', 'Issues that do not fit another category')
                    """);
            if (columnExists(connection, databaseName, "complaints", "category")) {
                statement.executeUpdate("""
                        UPDATE complaints c
                        JOIN complaint_categories cc
                          ON cc.name = LEFT(COALESCE(NULLIF(TRIM(c.category), ''), 'Other'), 80)
                        SET c.category_id = cc.id
                        WHERE c.category_id IS NULL
                        """);
            }
            statement.executeUpdate("""
                    UPDATE complaints
                    SET category_id = (SELECT id FROM complaint_categories WHERE name = 'Other' LIMIT 1)
                    WHERE category_id IS NULL
                    """);
            statement.executeUpdate("""
                    UPDATE complaints
                    SET complaint_code = CONCAT('CMP-LEGACY-', id)
                    WHERE complaint_code IS NULL OR TRIM(complaint_code) = ''
                    """);
            statement.executeUpdate("""
                    UPDATE complaints
                    SET title = 'Imported complaint'
                    WHERE title IS NULL OR TRIM(title) = ''
                    """);
            statement.executeUpdate("""
                    UPDATE complaints
                    SET description = 'Imported from an existing complaint record'
                    WHERE description IS NULL
                    """);
            statement.executeUpdate("""
                    UPDATE complaints
                    SET priority = CASE UPPER(COALESCE(priority, 'MEDIUM'))
                        WHEN 'LOW' THEN 'LOW' WHEN 'MEDIUM' THEN 'MEDIUM'
                        WHEN 'HIGH' THEN 'HIGH' WHEN 'URGENT' THEN 'URGENT'
                        WHEN 'CRITICAL' THEN 'URGENT' ELSE 'MEDIUM' END
                    """);
            statement.executeUpdate("""
                    UPDATE complaints
                    SET status = CASE UPPER(COALESCE(status, 'PENDING'))
                        WHEN 'OPEN' THEN 'PENDING' WHEN 'NEW' THEN 'PENDING'
                        WHEN 'PENDING' THEN 'PENDING' WHEN 'IN_PROGRESS' THEN 'IN_PROGRESS'
                        WHEN 'RESOLVED' THEN 'RESOLVED' WHEN 'CLOSED' THEN 'RESOLVED'
                        WHEN 'COMPLETED' THEN 'RESOLVED' WHEN 'REJECTED' THEN 'REJECTED'
                        ELSE 'PENDING' END
                    """);
            statement.executeUpdate("UPDATE complaints SET created_at = CURRENT_TIMESTAMP WHERE created_at IS NULL");
            statement.executeUpdate("UPDATE complaints SET updated_at = created_at WHERE updated_at IS NULL");
        }

        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE complaints MODIFY complaint_code VARCHAR(32) NOT NULL");
            statement.execute("ALTER TABLE complaints MODIFY category_id " + categoryIdType + " NOT NULL");
            statement.execute("ALTER TABLE complaints MODIFY title VARCHAR(180) NOT NULL");
            statement.execute("ALTER TABLE complaints MODIFY description TEXT NOT NULL");
            statement.execute("ALTER TABLE complaints MODIFY priority ENUM('LOW','MEDIUM','HIGH','URGENT') NOT NULL DEFAULT 'MEDIUM'");
            statement.execute("ALTER TABLE complaints MODIFY status ENUM('PENDING','IN_PROGRESS','RESOLVED','REJECTED') NOT NULL DEFAULT 'PENDING'");
            statement.execute("ALTER TABLE complaints MODIFY created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");
            statement.execute("ALTER TABLE complaints MODIFY updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
            statement.executeUpdate("""
                    UPDATE complaints
                    SET resolved_at = COALESCE(updated_at, created_at, CURRENT_TIMESTAMP)
                    WHERE status = 'RESOLVED' AND resolved_at IS NULL
                    """);
        }
        makeLegacyColumnNullable(connection, databaseName, "category");
        makeLegacyColumnNullable(connection, databaseName, "subject");

        ensureUniqueIndex(connection, databaseName, "complaints", "uq_complaints_code", "complaint_code");
        ensureForeignKey(connection, databaseName, "complaints", "category_id", "complaint_categories",
                "fk_complaint_category", "RESTRICT");
        ensureForeignKey(connection, databaseName, "complaints", "assigned_admin_id", "users",
                "fk_complaint_admin", "SET NULL");
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO complaint_updates (complaint_id, actor_id, old_status, new_status, remarks)
                    SELECT c.id, c.user_id, NULL, c.status, 'Imported from existing complaint record'
                    FROM complaints c
                    LEFT JOIN complaint_updates u ON u.complaint_id = c.id
                    WHERE u.id IS NULL
                    """);
        }
    }

    private static void makeLegacyColumnNullable(Connection connection, String databaseName, String column)
            throws SQLException {
        String type = columnType(connection, databaseName, "complaints", column);
        if (type == null) return;
        if (!type.matches("(?i)^(VARCHAR|CHAR)\\(\\d+\\)$")) {
            throw new SQLException("Cannot safely migrate legacy complaints." + column + " type " + type + ".");
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE complaints MODIFY `" + column + "` " + type + " NULL");
        }
    }

    private static void addColumns(Connection connection, String databaseName, String table,
                                   List<ColumnDefinition> columns) throws SQLException {
        for (ColumnDefinition column : columns) {
            if (columnExists(connection, databaseName, table, column.name())) continue;
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE `" + table + "` ADD COLUMN `" + column.name() + "` " + column.definition());
            }
        }
    }

    private static void ensureUniqueIndex(Connection connection, String databaseName, String table,
                                          String indexName, String column) throws SQLException {
        try (var query = connection.prepareStatement("""
                SELECT 1 FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND INDEX_NAME = ?
                """)) {
            query.setString(1, databaseName);
            query.setString(2, table);
            query.setString(3, indexName);
            try (var result = query.executeQuery()) {
                if (result.next()) return;
            }
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE UNIQUE INDEX `" + indexName + "` ON `" + table + "` (`" + column + "`)");
        }
    }

    private static void ensureForeignKey(Connection connection, String databaseName, String table, String column,
                                         String parentTable, String constraintName, String onDelete)
            throws SQLException {
        try (var query = connection.prepareStatement("""
                SELECT 1 FROM information_schema.KEY_COLUMN_USAGE
                WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = ?
                  AND REFERENCED_TABLE_NAME = ?
                """)) {
            query.setString(1, databaseName);
            query.setString(2, table);
            query.setString(3, column);
            query.setString(4, parentTable);
            try (var result = query.executeQuery()) {
                if (result.next()) return;
            }
        }
        String deleteRule = "SET NULL".equals(onDelete) ? "SET NULL" : "RESTRICT";
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE `" + table + "` ADD CONSTRAINT `" + constraintName + "` "
                    + "FOREIGN KEY (`" + column + "`) REFERENCES `" + parentTable + "` (`id`) ON DELETE " + deleteRule);
        }
    }

    private static boolean columnExists(Connection connection, String databaseName, String table, String column)
            throws SQLException {
        try (var query = connection.prepareStatement("""
                SELECT 1 FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = ?
                """)) {
            query.setString(1, databaseName);
            query.setString(2, table);
            query.setString(3, column);
            try (var result = query.executeQuery()) {
                return result.next();
            }
        }
    }

    private static String adaptForeignKeyColumnTypes(Connection connection, String databaseName, String command)
            throws SQLException {
        Matcher create = Pattern.compile("(?is)^CREATE\\s+TABLE\\s+IF\\s+NOT\\s+EXISTS\\s+([a-zA-Z0-9_]+)\\b")
                .matcher(command);
        if (!create.find()) return command;
        Map<String, String> references = FOREIGN_KEY_COLUMNS.get(create.group(1).toLowerCase());
        if (references == null) return command;

        String adapted = command;
        for (Map.Entry<String, String> entry : references.entrySet()) {
            String[] referenced = entry.getValue().split("\\.", 2);
            String parentType = columnType(connection, databaseName, referenced[0], referenced[1]);
            if (parentType == null) continue;
            Pattern column = Pattern.compile("(?i)(\\b" + Pattern.quote(entry.getKey())
                    + "\\s+)(?:BIGINT|INT|INTEGER|SMALLINT|MEDIUMINT|TINYINT)(?:\\s+UNSIGNED)?\\b");
            adapted = column.matcher(adapted).replaceAll(match -> match.group(1) + parentType);
        }
        return adapted;
    }

    private static String columnType(Connection connection, String databaseName, String table, String column)
            throws SQLException {
        try (var query = connection.prepareStatement("""
                SELECT COLUMN_TYPE FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = ?
                """)) {
            query.setString(1, databaseName);
            query.setString(2, table);
            query.setString(3, column);
            try (var result = query.executeQuery()) {
                return result.next() ? result.getString(1) : null;
            }
        }
    }

    private record ColumnDefinition(String name, String definition) {}
}
