package com.complaints.model;

public record User(long id, String fullName, String email, String role, boolean active) {
    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}
