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
public class OwnerDashboardResponse {

    private long totalSalons;
    private long totalBookingsThisMonth;
    private BigDecimal totalRevenueThisMonth;
    private long activeEmployees;
    private long upcomingAppointmentsCount;
}
