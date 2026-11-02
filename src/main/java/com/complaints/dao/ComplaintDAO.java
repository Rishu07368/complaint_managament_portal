package com.complaints.dao;

import com.complaints.config.Database;
import com.complaints.model.Complaint;
import com.complaints.model.ComplaintEvent;
import com.complaints.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public final class ComplaintDAO {
    private static final String SELECT = """
            SELECT c.id, c.complaint_code, c.user_id, u.full_name AS submitted_by, c.category_id,
                   cc.name AS category, c.title, c.description, c.priority, c.status,
                   c.assigned_admin_id, a.full_name AS assigned_admin, c.admin_remarks,
                   c.created_at, c.updated_at
            FROM complaints c
            JOIN users u ON u.id = c.user_id
            JOIN complaint_categories cc ON cc.id = c.category_id
            LEFT JOIN users a ON a.id = c.assigned_admin_id
            """;
    private final Database database;

    public ComplaintDAO(Database database) { this.database = database; }

    public List<Complaint> search(User viewer, String text, String status, String priority, Long categoryId) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT).append(" WHERE 1=1");
        List<Object> parameters = new ArrayList<>();
        if (!viewer.isAdmin()) { sql.append(" AND c.user_id = ?"); parameters.add(viewer.id()); }
        if (text != null && !text.isBlank()) {
            sql.append(" AND (c.complaint_code LIKE ? OR c.title LIKE ? OR c.description LIKE ? OR u.full_name LIKE ?)");
            String pattern = "%" + text.trim() + "%";
            parameters.add(pattern); parameters.add(pattern); parameters.add(pattern); parameters.add(pattern);
        }
        if (status != null && !status.isBlank()) { sql.append(" AND c.status = ?"); parameters.add(status); }
        if (priority != null && !priority.isBlank()) { sql.append(" AND c.priority = ?"); parameters.add(priority); }
        if (categoryId != null) { sql.append(" AND c.category_id = ?"); parameters.add(categoryId); }
        sql.append(" ORDER BY c.updated_at DESC LIMIT 500");
        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            bind(statement, parameters);
            try (ResultSet result = statement.executeQuery()) {
                List<Complaint> complaints = new ArrayList<>();
                while (result.next()) complaints.add(map(result));
                return complaints;
            }
        }
    }

    public Complaint findById(long id, User viewer) throws SQLException {
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(SELECT + " WHERE c.id = ?"
                     + (viewer.isAdmin() ? "" : " AND c.user_id = ?"))) {
            statement.setLong(1, id);
            if (!viewer.isAdmin()) statement.setLong(2, viewer.id());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new SQLException("Complaint was not found or you do not have permission to view it.");
                return map(result);
            }
        }
    }

    public String create(User user, long categoryId, String title, String description, String priority) throws SQLException {
        String code = "CMP-" + java.time.LocalDate.now().toString().replace("-", "") + "-"
                + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String sql = "INSERT INTO complaints (complaint_code, user_id, category_id, title, description, priority) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = database.connect()) {
            connection.setAutoCommit(false);
            try (PreparedStatement insert = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                insert.setString(1, code); insert.setLong(2, user.id()); insert.setLong(3, categoryId);
                insert.setString(4, title); insert.setString(5, description); insert.setString(6, priority);
                insert.executeUpdate();
                long id;
                try (ResultSet keys = insert.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("Unable to retrieve the complaint ID.");
                    id = keys.getLong(1);
                }
                addUpdate(connection, id, user.id(), null, "PENDING", "Complaint submitted");
                log(connection, user.id(), id, "COMPLAINT_CREATED", code);
                connection.commit();
                return code;
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    public void updateOwner(User user, long id, long categoryId, String title, String description,
                            String priority) throws SQLException {
        try (Connection connection = database.connect()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE complaints SET category_id = ?, title = ?, description = ?, priority = ?, updated_at = CURRENT_TIMESTAMP "
                                + "WHERE id = ? AND user_id = ? AND status = 'PENDING'")) {
                    statement.setLong(1, categoryId); statement.setString(2, title); statement.setString(3, description);
                    statement.setString(4, priority); statement.setLong(5, id); statement.setLong(6, user.id());
                    if (statement.executeUpdate() != 1) throw new SQLException("Only your pending complaints can be edited.");
                }
                addUpdate(connection, id, user.id(), "PENDING", "PENDING", "Complaint details updated");
                log(connection, user.id(), id, "COMPLAINT_EDITED", "Complaint details updated");
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    public void updateAdmin(User actor, Complaint complaint, long categoryId, String title, String description,
                            String status, String priority, Long assignedAdminId, String remarks) throws SQLException {
        long id = complaint.id();
        try (Connection connection = database.connect()) {
            connection.setAutoCommit(false);
            try {
                String oldStatus;
                try (PreparedStatement query = connection.prepareStatement("SELECT status, assigned_admin_id FROM complaints WHERE id = ? FOR UPDATE")) {
                    query.setLong(1, id);
                    try (ResultSet result = query.executeQuery()) {
                        if (!result.next()) throw new SQLException("Complaint no longer exists.");
                        oldStatus = result.getString("status");
                        long priorAdminId = result.getLong("assigned_admin_id");
                        Long previous = result.wasNull() ? null : priorAdminId;
                        if (!java.util.Objects.equals(previous, assignedAdminId) && assignedAdminId != null) {
                            try (PreparedStatement assign = connection.prepareStatement(
                                    "INSERT INTO complaint_assignments (complaint_id, admin_id, assigned_by) VALUES (?, ?, ?)")) {
                                assign.setLong(1, id); assign.setLong(2, assignedAdminId); assign.setLong(3, actor.id()); assign.executeUpdate();
                            }
                        }
                    }
                }
                try (PreparedStatement update = connection.prepareStatement("""
                        UPDATE complaints SET category_id = ?, title = ?, description = ?, status = ?, priority = ?,
                            assigned_admin_id = ?, admin_remarks = ?,
                            resolved_at = CASE WHEN ? = 'RESOLVED' THEN COALESCE(resolved_at, CURRENT_TIMESTAMP) ELSE NULL END,
                            updated_at = CURRENT_TIMESTAMP WHERE id = ?
                        """)) {
                    update.setLong(1, categoryId); update.setString(2, title); update.setString(3, description);
                    update.setString(4, status); update.setString(5, priority);
                    if (assignedAdminId == null) update.setNull(6, Types.BIGINT); else update.setLong(6, assignedAdminId);
                    update.setString(7, remarks); update.setString(8, status); update.setLong(9, id);
                    update.executeUpdate();
                }
                addUpdate(connection, id, actor.id(), oldStatus, status, remarks);
                log(connection, actor.id(), id, "COMPLAINT_UPDATED", "Status: " + oldStatus + " → " + status);
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    public void delete(User actor, long id) throws SQLException {
        String sql = actor.isAdmin() ? "DELETE FROM complaints WHERE id = ?"
                : "DELETE FROM complaints WHERE id = ? AND user_id = ? AND status = 'PENDING'";
        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            if (!actor.isAdmin()) statement.setLong(2, actor.id());
            if (statement.executeUpdate() != 1) throw new SQLException("Only administrators or owners of pending complaints can delete this complaint.");
        }
    }

    public List<ComplaintEvent> history(long id, User viewer) throws SQLException {
        String sql = """
                SELECT u.full_name, h.old_status, h.new_status, h.remarks, h.created_at
                FROM complaint_updates h JOIN users u ON u.id = h.actor_id
                JOIN complaints c ON c.id = h.complaint_id
                WHERE h.complaint_id = ?
                """ + (viewer.isAdmin() ? "" : " AND c.user_id = ?") + " ORDER BY h.created_at DESC";
        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            if (!viewer.isAdmin()) statement.setLong(2, viewer.id());
            try (ResultSet result = statement.executeQuery()) {
                List<ComplaintEvent> events = new ArrayList<>();
                while (result.next()) events.add(new ComplaintEvent(result.getString(1), result.getString(2),
                        result.getString(3), result.getString(4), result.getTimestamp(5).toLocalDateTime()));
                return events;
            }
        }
    }

    private static void addUpdate(Connection connection, long complaintId, long actorId, String oldStatus,
                                  String newStatus, String remarks) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO complaint_updates (complaint_id, actor_id, old_status, new_status, remarks) VALUES (?, ?, ?, ?, ?)")) {
            statement.setLong(1, complaintId); statement.setLong(2, actorId);
            statement.setString(3, oldStatus); statement.setString(4, newStatus); statement.setString(5, remarks);
            statement.executeUpdate();
        }
    }

    private static void log(Connection connection, long userId, long complaintId, String action, String details) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO activity_logs (user_id, complaint_id, action, details) VALUES (?, ?, ?, ?)")) {
            statement.setLong(1, userId); statement.setLong(2, complaintId);
            statement.setString(3, action); statement.setString(4, details); statement.executeUpdate();
        }
    }

    private static void bind(PreparedStatement statement, List<Object> parameters) throws SQLException {
        for (int i = 0; i < parameters.size(); i++) {
            Object value = parameters.get(i);
            if (value instanceof Long number) statement.setLong(i + 1, number);
            else statement.setString(i + 1, value.toString());
        }
    }

    private static Complaint map(ResultSet result) throws SQLException {
        long assigned = result.getLong("assigned_admin_id");
        Long assignedId = result.wasNull() ? null : assigned;
        Timestamp created = result.getTimestamp("created_at");
        Timestamp updated = result.getTimestamp("updated_at");
        return new Complaint(result.getLong("id"), result.getString("complaint_code"), result.getLong("user_id"),
                result.getString("submitted_by"), result.getLong("category_id"), result.getString("category"),
                result.getString("title"), result.getString("description"), result.getString("priority"),
                result.getString("status"), assignedId, result.getString("assigned_admin"),
                result.getString("admin_remarks"), created.toLocalDateTime(), updated.toLocalDateTime());
    }
}
