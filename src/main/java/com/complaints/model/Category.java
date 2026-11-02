package com.complaints.model;

public record Category(long id, String name, String description, boolean active) {
    @Override public String toString() { return name; }
}
