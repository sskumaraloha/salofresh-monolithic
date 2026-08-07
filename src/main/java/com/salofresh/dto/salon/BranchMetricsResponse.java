package com.salofresh.dto.salon;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchMetricsResponse {

    private Long salonId;
    private String salonName;
    private String city;
    private long totalBookingsThisMonth;
    private long completedBookingsThisMonth;
    private long cancelledBookingsThisMonth;
    private BigDecimal totalRevenueThisMonth;
    private long activeEmployees;
    private long upcomingAppointmentsCount;
    private double ratingAverage;
    private int reviewCount;
}
