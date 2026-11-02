package com.complaints.util;

import java.util.regex.Pattern;

public final class Validation {
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private Validation() {}

    public static String required(String value, String label, int maxLength) {
        String clean = value == null ? "" : value.trim();
        if (clean.isBlank()) throw new IllegalArgumentException(label + " is required.");
        if (clean.length() > maxLength) throw new IllegalArgumentException(label + " must be no more than " + maxLength + " characters.");
        return clean;
    }

    public static String email(String value) {
        String email = required(value, "Email", 190).toLowerCase();
        if (!EMAIL.matcher(email).matches()) throw new IllegalArgumentException("Enter a valid email address.");
        return email;
    }

    public static void password(char[] password) {
        if (password == null || password.length < 8) throw new IllegalArgumentException("Password must contain at least 8 characters.");
        if (password.length > 72) throw new IllegalArgumentException("Password must be no more than 72 characters.");
    }
}
