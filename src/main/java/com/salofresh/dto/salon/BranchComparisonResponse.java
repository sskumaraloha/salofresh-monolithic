package com.salofresh.dto.salon;

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
public class BranchComparisonResponse {

    private long totalSalons;
    private BigDecimal totalRevenueThisMonthAcrossBranches;
    private long totalBookingsThisMonthAcrossBranches;
    private Long topPerformingSalonId;
    private String topPerformingSalonName;
    private List<BranchMetricsResponse> branches;
}
