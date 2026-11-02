package com.complaints.controller;

import com.complaints.model.*;
import com.complaints.service.*;
import com.complaints.dao.UserDAO;

import java.sql.SQLException;
import java.util.List;

public final class ApplicationController {
    private final AuthenticationService authentication;
    private final ComplaintService complaints;
    private final CategoryService categories;
    private final UserService users;
    private final ReportService reports;
    private User session;

    public ApplicationController(AuthenticationService authentication, ComplaintService complaints,
                                 CategoryService categories, UserService users, ReportService reports) {
        this.authentication = authentication; this.complaints = complaints;
        this.categories = categories; this.users = users; this.reports = reports;
    }

    public User login(String email, char[] password) throws SQLException {
        session = authentication.login(email, password);
        return session;
    }

    public User register(String name, String email, char[] password) throws SQLException {
        return authentication.register(name, email, password);
    }

    public User session() { return session; }
    public void logout() { session = null; }
    public List<Category> categories() throws SQLException { return categories.list(); }
    public List<Category> managedCategories() throws SQLException { return categories.manage(requireSession()); }
    public void saveCategory(Category category) throws SQLException { categories.save(requireSession(), category); }
    public void deactivateCategory(long id) throws SQLException { categories.deactivate(requireSession(), id); }
    public List<User> users() throws SQLException { return users.all(requireSession()); }
    public User updateProfile(String name, String email) throws SQLException {
        session = users.updateProfile(requireSession(), name, email);
        return session;
    }
    public void changePassword(char[] current, char[] updated) throws SQLException {
        users.changePassword(requireSession(), current, updated);
    }
    public List<Complaint> search(String text, String status, String priority, Long categoryId) throws SQLException {
        return complaints.search(requireSession(), text, status, priority, categoryId);
    }
    public Complaint complaint(long id) throws SQLException { return complaints.details(requireSession(), id); }
    public List<ComplaintEvent> history(long id) throws SQLException { return complaints.history(requireSession(), id); }
    public String createComplaint(long categoryId, String title, String description, String priority) throws SQLException {
        return complaints.create(requireSession(), categoryId, title, description, priority);
    }
    public void updateComplaint(Complaint complaint, long categoryId, String title, String description,
                               String priority, String status, Long assignedAdminId, String remarks) throws SQLException {
        complaints.update(requireSession(), complaint, categoryId, title, description, priority, status, assignedAdminId, remarks);
    }
    public void deleteComplaint(long id) throws SQLException { complaints.delete(requireSession(), id); }
    public DashboardData dashboard() throws SQLException { return reports.dashboard(requireSession()); }
    public ReportData report() throws SQLException { return reports.report(requireSession()); }

    private User requireSession() {
        if (session == null) throw new SecurityException("Your session has ended. Please sign in again.");
        return session;
    }
}
