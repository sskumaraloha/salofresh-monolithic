package com.salofresh.dto.admin;

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
public class PlatformDashboardResponse {

    private long totalUsers;
    private long totalCustomers;
    private long totalSalonOwners;
    private long totalSalons;
    private long pendingSalonApprovals;
    private long totalBookingsThisMonth;
    private BigDecimal totalRevenueThisMonth;
    private long totalActiveEmployeesAcrossPlatform;
}
