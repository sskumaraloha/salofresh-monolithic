package com.salofresh.dto.platformbilling;

import com.salofresh.common.enums.BillingCycle;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Public-facing summary of a {@link com.salofresh.entity.PlatformPlan} that SaloFresh sells to
 * salon owners for the right to use the platform itself. Not to be confused with a salon's own
 * customer-facing membership plans.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformPlanResponse {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private BillingCycle billingCycle;

    /**
     * Maximum number of salons this plan allows the owner to operate. {@code null} means
     * unlimited; see {@link #unlimitedSalons}.
     */
    private Integer maxSalons;

    private boolean unlimitedSalons;

    private BigDecimal commissionPercentage;
}
