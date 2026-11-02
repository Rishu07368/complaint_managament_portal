package com.complaints.model;

import java.time.LocalDateTime;

public record ComplaintEvent(String actor, String oldStatus, String newStatus, String remarks,
                             LocalDateTime createdAt) {}
