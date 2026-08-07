package com.salofresh.dto.admin;

import com.salofresh.common.enums.BillingCycle;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Admin-facing view of a {@link com.salofresh.entity.PlatformPlan}, used for the platform
 * monetization / commission configuration screens (as opposed to
 * {@link com.salofresh.dto.platformbilling.PlatformPlanResponse}, which is the salon-owner-facing
 * subscribe-to-a-plan view).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformPlanAdminResponse {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private BillingCycle billingCycle;
    private Integer maxSalons;
    private BigDecimal commissionPercentage;
    private boolean active;
}
