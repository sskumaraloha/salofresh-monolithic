package com.salofresh.dto.payout;

import com.salofresh.common.enums.PayoutStatus;
import com.salofresh.dto.salon.SalonSummaryResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalonPayoutResponse {

    private Long id;
    private SalonSummaryResponse salon;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal grossRevenue;
    private BigDecimal platformCommissionAmount;
    private BigDecimal netPayoutAmount;
    private BigDecimal commissionPercentageApplied;
    private PayoutStatus status;
    private String payoutReference;
    private Instant processedAt;
}
