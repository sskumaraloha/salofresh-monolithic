package com.salofresh.service.admin;

import com.salofresh.dto.admin.BookingReportResponse;
import com.salofresh.dto.admin.CustomerReportResponse;
import com.salofresh.dto.admin.PlatformDashboardResponse;
import com.salofresh.dto.admin.ReportDateRangeRequest;
import com.salofresh.dto.admin.RevenueReportResponse;
import com.salofresh.dto.admin.SalonReportResponse;

public interface AnalyticsService {

    PlatformDashboardResponse getPlatformDashboard();

    RevenueReportResponse getRevenueReport(ReportDateRangeRequest request);

    BookingReportResponse getBookingReport(ReportDateRangeRequest request);

    SalonReportResponse getSalonReport(ReportDateRangeRequest request);

    CustomerReportResponse getCustomerReport(ReportDateRangeRequest request);
}
