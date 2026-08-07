package com.salofresh.dto.admin;

import com.salofresh.common.enums.BillingCycle;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Patch-semantics update request for a {@link com.salofresh.entity.PlatformPlan}: every field is
 * optional, and only non-null fields are applied by the service. To explicitly set
 * {@code maxSalons} to "unlimited", the client must use a separate mechanism (this DTO cannot
 * distinguish "leave unchanged" from "set to null" for that field).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePlatformPlanRequest {

    private String name;

    private String description;

    @Positive(message = "Price must be positive")
    private BigDecimal price;

    private BillingCycle billingCycle;

    private Integer maxSalons;

    @DecimalMin(value = "0", message = "Commission percentage must be at least 0")
    @DecimalMax(value = "100", message = "Commission percentage must not exceed 100")
    private BigDecimal commissionPercentage;
}
