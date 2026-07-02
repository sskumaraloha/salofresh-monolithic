package com.salofresh.service.impl.admin;

import com.salofresh.common.enums.VerificationStatus;
import com.salofresh.dto.admin.BookingReportResponse;
import com.salofresh.dto.admin.CustomerReportResponse;
import com.salofresh.dto.admin.PlatformDashboardResponse;
import com.salofresh.dto.admin.ReportDateRangeRequest;
import com.salofresh.dto.admin.ReportGroupBy;
import com.salofresh.dto.admin.RevenueReportResponse;
import com.salofresh.dto.admin.SalonReportResponse;
import com.salofresh.exception.BadRequestException;
import com.salofresh.repository.AnalyticsQueryRepository;
import com.salofresh.service.admin.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsServiceImpl implements AnalyticsService {

    private static final int TOP_SALONS_LIMIT = 10;

    private final AnalyticsQueryRepository analyticsQueryRepository;

    @Override
    public PlatformDashboardResponse getPlatformDashboard() {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);

        return PlatformDashboardResponse.builder()
                .totalUsers(analyticsQueryRepository.countTotalUsers())
                .totalCustomers(analyticsQueryRepository.countTotalCustomers())
                .totalSalonOwners(analyticsQueryRepository.countTotalSalonOwners())
                .totalSalons(analyticsQueryRepository.countTotalSalons())
                .pendingSalonApprovals(analyticsQueryRepository.countSalonsByVerificationStatus(VerificationStatus.PENDING))
                .totalBookingsThisMonth(analyticsQueryRepository.countBookingsBetween(monthStart, today))
                .totalRevenueThisMonth(analyticsQueryRepository.totalRevenueBetween(monthStart, today))
                .totalActiveEmployeesAcrossPlatform(analyticsQueryRepository.countActiveEmployeesAcrossPlatform())
                .build();
    }

    @Override
    public RevenueReportResponse getRevenueReport(ReportDateRangeRequest request) {
        LocalDate from = validatedFrom(request);
        LocalDate to = validatedTo(request);
        ReportGroupBy groupBy = groupByOrDefault(request);

        List<Object[]> rows = analyticsQueryRepository.revenueByPeriod(from, to, groupBy);
        BigDecimal totalRevenue = BigDecimal.ZERO;
        long totalBookings = 0;
        List<RevenueReportResponse.RevenueDataPoint> points = new java.util.ArrayList<>();
        for (Object[] row : rows) {
            String period = String.valueOf(row[0]);
            BigDecimal revenue = toBigDecimal(row[1]);
            long bookingCount = toLong(row[2]);
            totalRevenue = totalRevenue.add(revenue);
            totalBookings += bookingCount;
            points.add(RevenueReportResponse.RevenueDataPoint.builder()
                    .period(period)
                    .revenue(revenue)
                    .bookingCount(bookingCount)
                    .build());
        }

        return RevenueReportResponse.builder()
                .data(points)
                .totalRevenue(totalRevenue)
                .totalBookings(totalBookings)
                .build();
    }

    @Override
    public BookingReportResponse getBookingReport(ReportDateRangeRequest request) {
        LocalDate from = validatedFrom(request);
        LocalDate to = validatedTo(request);
        ReportGroupBy groupBy = groupByOrDefault(request);

        List<Object[]> rows = analyticsQueryRepository.bookingCountsByPeriod(from, to, groupBy);
        long totalBookings = 0;
        List<BookingReportResponse.BookingDataPoint> points = new java.util.ArrayList<>();
        for (Object[] row : rows) {
            String period = String.valueOf(row[0]);
            long total = toLong(row[1]);
            long completed = toLong(row[2]);
            long cancelled = toLong(row[3]);
            long noShow = toLong(row[4]);
            totalBookings += total;
            points.add(BookingReportResponse.BookingDataPoint.builder()
                    .period(period)
                    .totalBookings(total)
                    .completed(completed)
                    .cancelled(cancelled)
                    .noShow(noShow)
                    .build());
        }

        return BookingReportResponse.builder()
                .data(points)
                .totalBookings(totalBookings)
                .build();
    }

    @Override
    public SalonReportResponse getSalonReport(ReportDateRangeRequest request) {
        LocalDate from = validatedFrom(request);
        LocalDate to = validatedTo(request);

        List<Object[]> rows = analyticsQueryRepository.topSalonsByRevenue(from, to, TOP_SALONS_LIMIT);
        List<SalonReportResponse.SalonDataPoint> points = new java.util.ArrayList<>();
        for (Object[] row : rows) {
            points.add(SalonReportResponse.SalonDataPoint.builder()
                    .salonId(((Number) row[0]).longValue())
                    .salonName(String.valueOf(row[1]))
                    .bookings(toLong(row[2]))
                    .revenue(toBigDecimal(row[3]))
                    .averageRating(row[4] == null ? 0.0 : ((Number) row[4]).doubleValue())
                    .build());
        }

        return SalonReportResponse.builder().data(points).build();
    }

    @Override
    public CustomerReportResponse getCustomerReport(ReportDateRangeRequest request) {
        LocalDate from = validatedFrom(request);
        LocalDate to = validatedTo(request);
        ReportGroupBy groupBy = groupByOrDefault(request);

        List<Object[]> newCustomerRows = analyticsQueryRepository.newCustomersByPeriod(from, to, groupBy);
        List<Object[]> activeCustomerRows = analyticsQueryRepository.activeCustomersByPeriod(from, to, groupBy);

        Map<String, long[]> merged = new LinkedHashMap<>();
        for (Object[] row : newCustomerRows) {
            String period = String.valueOf(row[0]);
            merged.computeIfAbsent(period, p -> new long[2])[0] = toLong(row[1]);
        }
        for (Object[] row : activeCustomerRows) {
            String period = String.valueOf(row[0]);
            merged.computeIfAbsent(period, p -> new long[2])[1] = toLong(row[1]);
        }

        List<CustomerReportResponse.CustomerDataPoint> points = merged.entrySet().stream()
                .map(entry -> CustomerReportResponse.CustomerDataPoint.builder()
                        .period(entry.getKey())
                        .newCustomers(entry.getValue()[0])
                        .activeCustomers(entry.getValue()[1])
                        .build())
                .sorted(java.util.Comparator.comparing(CustomerReportResponse.CustomerDataPoint::getPeriod))
                .toList();

        return CustomerReportResponse.builder().data(points).build();
    }

    private LocalDate validatedFrom(ReportDateRangeRequest request) {
        if (request.getFromDate() == null) {
            throw new BadRequestException("fromDate is required");
        }
        return request.getFromDate();
    }

    private LocalDate validatedTo(ReportDateRangeRequest request) {
        LocalDate to = request.getToDate() == null ? LocalDate.now() : request.getToDate();
        if (to.isBefore(request.getFromDate())) {
            throw new BadRequestException("toDate must not be before fromDate");
        }
        return to;
    }

    private ReportGroupBy groupByOrDefault(ReportDateRangeRequest request) {
        return request.getGroupBy() == null ? ReportGroupBy.MONTH : request.getGroupBy();
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        return new BigDecimal(value.toString());
    }

    private long toLong(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }
}
