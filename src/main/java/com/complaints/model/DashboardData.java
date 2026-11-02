package com.complaints.model;

import java.util.List;
import java.util.Map;

public record DashboardData(Map<String, Long> counts, Map<String, Long> statuses,
                            Map<String, Long> categories, List<Complaint> recent,
                            List<String> activity) {}
