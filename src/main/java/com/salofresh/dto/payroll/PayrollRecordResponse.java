package com.salofresh.dto.payroll;

import com.salofresh.common.enums.PayrollStatus;
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
public class PayrollRecordResponse {

    private Long id;
    private EmployeeSummary employee;
    private Long salonId;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal baseSalary;
    private BigDecimal commissionEarned;
    private BigDecimal deductions;
    private BigDecimal bonus;
    private BigDecimal netPay;
    private PayrollStatus status;
    private Instant paidAt;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmployeeSummary {
        private Long id;
        private String fullName;
        private String designation;
    }
}
