package com.salofresh.dto.payroll;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratePayrollRequest {

    @NotNull(message = "Employee id is required")
    private Long employeeId;

    @NotNull(message = "Period start is required")
    private LocalDate periodStart;

    @NotNull(message = "Period end is required")
    private LocalDate periodEnd;

    @NotNull(message = "Base salary is required")
    @PositiveOrZero(message = "Base salary must be zero or positive")
    private BigDecimal baseSalary;

    @PositiveOrZero(message = "Deductions must be zero or positive")
    @Builder.Default
    private BigDecimal deductions = BigDecimal.ZERO;

    @PositiveOrZero(message = "Bonus must be zero or positive")
    @Builder.Default
    private BigDecimal bonus = BigDecimal.ZERO;
}
