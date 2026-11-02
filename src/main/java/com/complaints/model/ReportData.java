package com.complaints.model;

import java.util.List;
import java.util.Map;

public record ReportData(Map<String, Long> categories, Map<String, Long> priorities,
                         Map<String, Long> statuses, List<MonthlyCount> monthly,
                         double resolutionRate, double averageResolutionHours) {
    public record MonthlyCount(String month, long count) {}
}
