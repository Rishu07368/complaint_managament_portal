package com.complaints.dao;

import com.complaints.config.Database;
import com.complaints.model.DashboardData;
import com.complaints.model.ReportData;
import com.complaints.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ReportDAO {
    private final Database database;
    private final ComplaintDAO complaints;

    public ReportDAO(Database database, ComplaintDAO complaints) {
        this.database = database;
        this.complaints = complaints;
    }

    public DashboardData dashboard(User viewer) throws SQLException {
        String scope = viewer.isAdmin() ? "" : " WHERE user_id = ?";
        Map<String, Long> counts = new LinkedHashMap<>();
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM complaints" + scope)) {
            if (!viewer.isAdmin()) statement.setLong(1, viewer.id());
            try (ResultSet result = statement.executeQuery()) { result.next(); counts.put("TOTAL", result.getLong(1)); }
        }
        String where = viewer.isAdmin() ? "" : " WHERE c.user_id = ?";
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement("SELECT c.status, COUNT(*) FROM complaints c" + where + " GROUP BY c.status")) {
            if (!viewer.isAdmin()) statement.setLong(1, viewer.id());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) counts.put(result.getString(1), result.getLong(2));
            }
        }
        Map<String, Long> statuses = new LinkedHashMap<>();
        for (String status : List.of("PENDING", "IN_PROGRESS", "RESOLVED", "REJECTED"))
            statuses.put(status, counts.getOrDefault(status, 0L));
        counts.putIfAbsent("PENDING", 0L); counts.putIfAbsent("IN_PROGRESS", 0L);
        counts.putIfAbsent("RESOLVED", 0L); counts.putIfAbsent("REJECTED", 0L);

        Map<String, Long> categories = new LinkedHashMap<>();
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT cc.name, COUNT(c.id) FROM complaint_categories cc
                     LEFT JOIN complaints c ON c.category_id = cc.id
                     """ + (viewer.isAdmin() ? "" : " AND c.user_id = ")
                     + (viewer.isAdmin() ? "" : "?") + " GROUP BY cc.id, cc.name ORDER BY COUNT(c.id) DESC, cc.name LIMIT 8")) {
            if (!viewer.isAdmin()) statement.setLong(1, viewer.id());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) categories.put(result.getString(1), result.getLong(2));
            }
        }
        List<String> activity = new ArrayList<>();
        String activitySql = "SELECT action, details, created_at FROM activity_logs"
                + (viewer.isAdmin() ? "" : " WHERE user_id = ?") + " ORDER BY created_at DESC LIMIT 6";
        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(activitySql)) {
            if (!viewer.isAdmin()) statement.setLong(1, viewer.id());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) activity.add(result.getString(1).replace('_', ' ') + " · "
                        + result.getString(2) + " · " + result.getTimestamp(3).toLocalDateTime().toString().replace('T', ' '));
            }
        }
        return new DashboardData(counts, statuses, categories, complaints.search(viewer, "", "", "", null), activity);
    }

    public ReportData report(User viewer) throws SQLException {
        return new ReportData(grouped(viewer, "cc.name", "complaint_categories cc ON cc.id = c.category_id"),
                grouped(viewer, "c.priority", null), grouped(viewer, "c.status", null),
                monthly(viewer), resolutionRate(viewer), averageResolutionHours(viewer));
    }

    private Map<String, Long> grouped(User viewer, String column, String join) throws SQLException {
        String sql = "SELECT " + column + ", COUNT(*) FROM complaints c"
                + (join == null ? "" : " JOIN " + join)
                + (viewer.isAdmin() ? "" : " WHERE c.user_id = ?")
                + " GROUP BY " + column + " ORDER BY COUNT(*) DESC";
        Map<String, Long> resultMap = new LinkedHashMap<>();
        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            if (!viewer.isAdmin()) statement.setLong(1, viewer.id());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) resultMap.put(result.getString(1), result.getLong(2));
            }
        }
        return resultMap;
    }

    private List<ReportData.MonthlyCount> monthly(User viewer) throws SQLException {
        String sql = "SELECT DATE_FORMAT(created_at, '%Y-%m') AS month, COUNT(*) FROM complaints"
                + (viewer.isAdmin() ? "" : " WHERE user_id = ?")
                + " GROUP BY DATE_FORMAT(created_at, '%Y-%m') ORDER BY month DESC LIMIT 12";
        List<ReportData.MonthlyCount> results = new ArrayList<>();
        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            if (!viewer.isAdmin()) statement.setLong(1, viewer.id());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) results.add(new ReportData.MonthlyCount(result.getString(1), result.getLong(2)));
            }
        }
        java.util.Collections.reverse(results);
        return results;
    }

    private double resolutionRate(User viewer) throws SQLException {
        String sql = "SELECT COUNT(*), SUM(status = 'RESOLVED') FROM complaints"
                + (viewer.isAdmin() ? "" : " WHERE user_id = ?");
        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            if (!viewer.isAdmin()) statement.setLong(1, viewer.id());
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getLong(1) == 0 ? 0 : result.getDouble(2) * 100.0 / result.getLong(1);
            }
        }
    }

    private double averageResolutionHours(User viewer) throws SQLException {
        String sql = "SELECT AVG(TIMESTAMPDIFF(MINUTE, created_at, resolved_at)) / 60.0 FROM complaints WHERE resolved_at IS NOT NULL"
                + (viewer.isAdmin() ? "" : " AND user_id = ?");
        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            if (!viewer.isAdmin()) statement.setLong(1, viewer.id());
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                double value = result.getDouble(1);
                return result.wasNull() ? 0 : value;
            }
        }
    }
}
