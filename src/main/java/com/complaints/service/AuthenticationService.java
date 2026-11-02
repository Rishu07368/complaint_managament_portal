package com.complaints.service;

import com.complaints.dao.UserDAO;
import com.complaints.model.User;
import com.complaints.util.Validation;

import java.sql.SQLException;

public final class AuthenticationService {
    private final UserDAO users;

    public AuthenticationService(UserDAO users) { this.users = users; }

    public User login(String email, char[] password) throws SQLException {
        String normalized = Validation.email(email);
        if (password == null || password.length == 0) throw new IllegalArgumentException("Password is required.");
        return users.authenticate(normalized, password).orElseThrow(() ->
                new IllegalArgumentException("Email or password is incorrect, or this account is inactive."));
    }

    public User register(String name, String email, char[] password) throws SQLException {
        Validation.password(password);
        return users.create(Validation.required(name, "Full name", 120), Validation.email(email), password);
    }
}
