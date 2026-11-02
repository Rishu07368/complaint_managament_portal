package com.complaints.dao;

import com.complaints.config.Database;
import com.complaints.model.Category;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public final class CategoryDAO {
    private final Database database;

    public CategoryDAO(Database database) { this.database = database; }

    public List<Category> findAll(boolean includeInactive) throws SQLException {
        List<Category> categories = new ArrayList<>();
        String sql = "SELECT id, name, description, active FROM complaint_categories"
                + (includeInactive ? "" : " WHERE active = TRUE") + " ORDER BY name";
        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) categories.add(new Category(result.getLong("id"), result.getString("name"),
                    result.getString("description"), result.getBoolean("active")));
        }
        return categories;
    }

    public void seedDefaults() throws SQLException {
        String[][] defaults = {
                {"Facilities", "Building, equipment, and maintenance concerns"},
                {"IT Support", "Accounts, devices, software, and network issues"},
                {"Billing", "Invoices, payments, and account charges"},
                {"Customer Service", "Service quality and communication"},
                {"Other", "Issues that do not fit another category"}
        };
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT IGNORE INTO complaint_categories (name, description) VALUES (?, ?)")) {
            for (String[] category : defaults) {
                statement.setString(1, category[0]);
                statement.setString(2, category[1]);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    public void save(Category category) throws SQLException {
        String sql = category.id() == 0
                ? "INSERT INTO complaint_categories (name, description, active) VALUES (?, ?, ?)"
                : "UPDATE complaint_categories SET name = ?, description = ?, active = ? WHERE id = ?";
        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, category.name());
            statement.setString(2, category.description());
            statement.setBoolean(3, category.active());
            if (category.id() != 0) statement.setLong(4, category.id());
            statement.executeUpdate();
        }
    }

    public void deactivate(long id) throws SQLException {
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE complaint_categories SET active = FALSE WHERE id = ?")) {
            statement.setLong(1, id);
            if (statement.executeUpdate() != 1) throw new SQLException("Category no longer exists.");
        }
    }
}
