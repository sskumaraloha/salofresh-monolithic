package com.salofresh.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueReportResponse {

    private List<RevenueDataPoint> data;
    private BigDecimal totalRevenue;
    private long totalBookings;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevenueDataPoint {
        private String period;
        private BigDecimal revenue;
        private long bookingCount;
    }
}
