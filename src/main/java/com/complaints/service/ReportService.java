package com.complaints.service;

import com.complaints.dao.ReportDAO;
import com.complaints.model.DashboardData;
import com.complaints.model.ReportData;
import com.complaints.model.User;

import java.sql.SQLException;

public final class ReportService {
    private final ReportDAO reports;
    public ReportService(ReportDAO reports) { this.reports = reports; }

    public DashboardData dashboard(User user) throws SQLException { return reports.dashboard(user); }

    public ReportData report(User user) throws SQLException {
        UserService.requireAdmin(user);
        return reports.report(user);
    }
}
