package com.salofresh.dto.payroll;

import com.salofresh.common.enums.CommissionRuleType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
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
public class CommissionRuleRequest {

    /**
     * Optional. When present, the rule is scoped to this specific employee.
     * When absent, the rule is salon-wide (applies to any employee without a more specific match).
     */
    private Long employeeId;

    /**
     * Optional. When present, the rule only applies to services under this category.
     * When absent, the rule applies regardless of service category.
     */
    private Long categoryId;

    @NotNull(message = "Rule type is required")
    private CommissionRuleType ruleType;

    @NotNull(message = "Value is required")
    @PositiveOrZero(message = "Value must be zero or positive")
    private BigDecimal value;

    private Boolean active;
}
