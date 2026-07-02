package com.salofresh.controller.admin;

import com.salofresh.constant.AppConstants;
import com.salofresh.dto.admin.BookingReportResponse;
import com.salofresh.dto.admin.CustomerReportResponse;
import com.salofresh.dto.admin.ReportDateRangeRequest;
import com.salofresh.dto.admin.ReportGroupBy;
import com.salofresh.dto.admin.RevenueReportResponse;
import com.salofresh.dto.admin.SalonReportResponse;
import com.salofresh.response.ApiResponse;
import com.salofresh.service.admin.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/admin/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
@Tag(name = "Admin - Reports", description = "Platform revenue, booking, salon and customer analytics reports")
public class AdminReportController {

    private final AnalyticsService analyticsService;

    @GetMapping("/revenue")
    @Operation(summary = "Revenue report grouped by day/week/month over a date range")
    public ResponseEntity<ApiResponse<RevenueReportResponse>> revenue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false, defaultValue = "MONTH") ReportGroupBy groupBy) {
        return ResponseEntity.ok(ApiResponse.success("Revenue report generated successfully",
                analyticsService.getRevenueReport(toRequest(fromDate, toDate, groupBy))));
    }

    @GetMapping("/bookings")
    @Operation(summary = "Booking report (with completed/cancelled/no-show breakdown) over a date range")
    public ResponseEntity<ApiResponse<BookingReportResponse>> bookings(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false, defaultValue = "MONTH") ReportGroupBy groupBy) {
        return ResponseEntity.ok(ApiResponse.success("Booking report generated successfully",
                analyticsService.getBookingReport(toRequest(fromDate, toDate, groupBy))));
    }

    @GetMapping("/salons")
    @Operation(summary = "Top salons report ranked by revenue over a date range")
    public ResponseEntity<ApiResponse<SalonReportResponse>> salons(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false, defaultValue = "MONTH") ReportGroupBy groupBy) {
        return ResponseEntity.ok(ApiResponse.success("Salon report generated successfully",
                analyticsService.getSalonReport(toRequest(fromDate, toDate, groupBy))));
    }

    @GetMapping("/customers")
    @Operation(summary = "Customer growth and activity report over a date range")
    public ResponseEntity<ApiResponse<CustomerReportResponse>> customers(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false, defaultValue = "MONTH") ReportGroupBy groupBy) {
        return ResponseEntity.ok(ApiResponse.success("Customer report generated successfully",
                analyticsService.getCustomerReport(toRequest(fromDate, toDate, groupBy))));
    }

    private ReportDateRangeRequest toRequest(LocalDate fromDate, LocalDate toDate, ReportGroupBy groupBy) {
        return ReportDateRangeRequest.builder()
                .fromDate(fromDate)
                .toDate(toDate)
                .groupBy(groupBy)
                .build();
    }
}
