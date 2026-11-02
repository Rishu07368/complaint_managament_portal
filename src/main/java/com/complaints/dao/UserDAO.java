package com.complaints.dao;

import com.complaints.config.Database;
import com.complaints.model.User;
import com.complaints.util.PasswordUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class UserDAO {
    private final Database database;

    public UserDAO(Database database) { this.database = database; }

    public Optional<User> authenticate(String email, char[] password) throws SQLException {
        String sql = "SELECT id, full_name, email, password_hash, role, active FROM users WHERE email = ?";
        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next() || !result.getBoolean("active")) return Optional.empty();
                String storedHash = result.getString("password_hash");
                if (!PasswordUtil.matches(password, storedHash)) return Optional.empty();
                User user = map(result);
                if (PasswordUtil.needsUpgrade(storedHash)) upgradePasswordHash(user.id(), storedHash, password);
                return Optional.of(user);
            }
        }
    }

    private void upgradePasswordHash(long userId, String previousHash, char[] password) throws SQLException {
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE users SET password_hash = ? WHERE id = ? AND password_hash = ?")) {
            statement.setString(1, PasswordUtil.hash(password));
            statement.setLong(2, userId);
            statement.setString(3, previousHash);
            statement.executeUpdate();
        }
    }

    public User create(String name, String email, char[] password) throws SQLException {
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO users (full_name, email, password_hash) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, name);
            statement.setString(2, email);
            statement.setString(3, PasswordUtil.hash(password));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Unable to retrieve the new user ID.");
                return new User(keys.getLong(1), name, email, "USER", true);
            }
        }
    }

    public void ensureAdministrator(String name, String email, char[] password) throws SQLException {
        try (Connection connection = database.connect();
             PreparedStatement check = connection.prepareStatement("SELECT id FROM users WHERE role = 'ADMIN' LIMIT 1");
             ResultSet result = check.executeQuery()) {
            if (result.next()) return;
        }
        try (Connection connection = database.connect();
             PreparedStatement insert = connection.prepareStatement(
                     "INSERT INTO users (full_name, email, password_hash, role) VALUES (?, ?, ?, 'ADMIN')")) {
            insert.setString(1, name);
            insert.setString(2, email);
            insert.setString(3, PasswordUtil.hash(password));
            insert.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException e) {
            if (!"23000".equals(e.getSQLState())) throw e;
        }
    }

    public List<User> findAll() throws SQLException {
        List<User> users = new ArrayList<>();
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id, full_name, email, role, active FROM users ORDER BY created_at DESC");
             ResultSet result = statement.executeQuery()) {
            while (result.next()) users.add(map(result));
        }
        return users;
    }

    public void updateProfile(User user, String name, String email) throws SQLException {
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE users SET full_name = ?, email = ? WHERE id = ? AND active = TRUE")) {
            statement.setString(1, name);
            statement.setString(2, email);
            statement.setLong(3, user.id());
            if (statement.executeUpdate() != 1) throw new SQLException("The user profile could not be updated.");
        }
    }

    public boolean changePassword(User user, char[] currentPassword, char[] newPassword) throws SQLException {
        try (Connection connection = database.connect();
             PreparedStatement query = connection.prepareStatement("SELECT password_hash FROM users WHERE id = ? AND active = TRUE")) {
            query.setLong(1, user.id());
            try (ResultSet result = query.executeQuery()) {
                if (!result.next() || !PasswordUtil.matches(currentPassword, result.getString(1))) return false;
            }
        }
        try (Connection connection = database.connect();
             PreparedStatement update = connection.prepareStatement("UPDATE users SET password_hash = ? WHERE id = ?")) {
            update.setString(1, PasswordUtil.hash(newPassword));
            update.setLong(2, user.id());
            return update.executeUpdate() == 1;
        }
    }

    private static User map(ResultSet result) throws SQLException {
        return new User(result.getLong("id"), result.getString("full_name"),
                result.getString("email"), result.getString("role"), result.getBoolean("active"));
    }
}
