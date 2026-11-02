package com.complaints.service;

import com.complaints.dao.CategoryDAO;
import com.complaints.dao.ComplaintDAO;
import com.complaints.dao.UserDAO;
import com.complaints.model.*;
import com.complaints.util.Validation;

import java.sql.SQLException;
import java.util.List;

public final class ComplaintService {
    private static final List<String> PRIORITIES = List.of("LOW", "MEDIUM", "HIGH", "URGENT");
    private static final List<String> STATUSES = List.of("PENDING", "IN_PROGRESS", "RESOLVED", "REJECTED");
    private final ComplaintDAO complaints;
    private final CategoryDAO categories;
    private final UserDAO users;

    public ComplaintService(ComplaintDAO complaints, CategoryDAO categories, UserDAO users) {
        this.complaints = complaints; this.categories = categories; this.users = users;
    }

    public List<Complaint> search(User viewer, String query, String status, String priority, Long categoryId) throws SQLException {
        return complaints.search(viewer, query, status, priority, categoryId);
    }

    public Complaint details(User viewer, long id) throws SQLException { return complaints.findById(id, viewer); }

    public String create(User owner, long categoryId, String title, String description, String priority) throws SQLException {
        validateCategory(categoryId);
        validatePriority(priority);
        String validTitle = Validation.required(title, "Title", 180);
        String validDescription = Validation.required(description, "Description", 10000);
        return complaints.create(owner, categoryId, validTitle, validDescription, priority);
    }

    public void update(User actor, Complaint complaint, long categoryId, String title, String description,
                       String priority, String status, Long assignedAdminId, String remarks) throws SQLException {
        validateCategory(categoryId);
        validatePriority(priority);
        String validTitle = Validation.required(title, "Title", 180);
        String validDescription = Validation.required(description, "Description", 10000);
        if (actor.isAdmin()) {
            UserService.requireAdmin(actor);
            if (!STATUSES.contains(status)) throw new IllegalArgumentException("Select a valid complaint status.");
            String validRemarks = remarks == null ? "" : remarks.trim();
            if (validRemarks.length() > 10000) throw new IllegalArgumentException("Administrative remarks are too long.");
            if (assignedAdminId != null && users.findAll().stream().noneMatch(u -> u.id() == assignedAdminId && u.isAdmin() && u.active()))
                throw new IllegalArgumentException("Select an active administrator for assignment.");
            complaints.updateAdmin(actor, complaint, categoryId, validTitle, validDescription,
                    status, priority, assignedAdminId, validRemarks);
        } else {
            if (complaint.userId() != actor.id()) throw new SecurityException("You can only edit your own complaints.");
            if (!"PENDING".equals(complaint.status())) throw new IllegalArgumentException("Only pending complaints can be edited.");
            complaints.updateOwner(actor, complaint.id(), categoryId, validTitle, validDescription, priority);
        }
    }

    public void delete(User actor, long id) throws SQLException { complaints.delete(actor, id); }

    public List<ComplaintEvent> history(User viewer, long id) throws SQLException {
        complaints.findById(id, viewer);
        return complaints.history(id, viewer);
    }

    private void validateCategory(long id) throws SQLException {
        if (categories.findAll(false).stream().noneMatch(category -> category.id() == id))
            throw new IllegalArgumentException("Select an active complaint category.");
    }

    private static void validatePriority(String priority) {
        if (!PRIORITIES.contains(priority)) throw new IllegalArgumentException("Select a valid priority.");
    }
}
