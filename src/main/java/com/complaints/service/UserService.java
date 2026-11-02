package com.complaints.service;

import com.complaints.dao.UserDAO;
import com.complaints.model.User;
import com.complaints.util.Validation;

import java.sql.SQLException;
import java.util.List;

public final class UserService {
    private final UserDAO users;

    public UserService(UserDAO users) { this.users = users; }

    public List<User> all(User actor) throws SQLException {
        requireAdmin(actor);
        return users.findAll();
    }

    public User updateProfile(User user, String name, String email) throws SQLException {
        String validName = Validation.required(name, "Full name", 120);
        String validEmail = Validation.email(email);
        users.updateProfile(user, validName, validEmail);
        return new User(user.id(), validName, validEmail, user.role(), user.active());
    }

    public void changePassword(User user, char[] current, char[] updated) throws SQLException {
        Validation.password(updated);
        if (!users.changePassword(user, current, updated)) throw new IllegalArgumentException("Current password is incorrect.");
    }

    public static void requireAdmin(User user) {
        if (!user.isAdmin()) throw new SecurityException("Administrator access is required.");
    }
}
