package com.salofresh.dto.payroll;

import com.salofresh.common.enums.CommissionRuleType;
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
public class CommissionRuleResponse {

    private Long id;
    private Long salonId;
    private Long employeeId;
    private String employeeName;
    private Long categoryId;
    private String categoryName;
    private CommissionRuleType ruleType;
    private BigDecimal value;
    private boolean active;
}
