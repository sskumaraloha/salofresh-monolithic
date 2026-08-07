package com.salofresh.dto.admin;

import com.salofresh.common.enums.BillingCycle;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
public class CreatePlatformPlanRequest {

    @NotBlank(message = "Plan name is required")
    private String name;

    private String description;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    private BigDecimal price;

    @NotNull(message = "Billing cycle is required")
    private BillingCycle billingCycle;

    /**
     * Maximum number of salons a subscribing owner may operate under this plan. {@code null}
     * means unlimited.
     */
    private Integer maxSalons;

    @NotNull(message = "Commission percentage is required")
    @DecimalMin(value = "0", message = "Commission percentage must be at least 0")
    @DecimalMax(value = "100", message = "Commission percentage must not exceed 100")
    private BigDecimal commissionPercentage;
}
