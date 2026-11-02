package com.complaints.model;

import java.time.LocalDateTime;

public record Complaint(long id, String code, long userId, String submittedBy, long categoryId,
                        String category, String title, String description, String priority,
                        String status, Long assignedAdminId, String assignedAdmin,
                        String remarks, LocalDateTime createdAt, LocalDateTime updatedAt) {}
