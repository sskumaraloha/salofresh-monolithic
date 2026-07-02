package com.salofresh.dto.employee;

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
public class EmployeePerformanceResponse {

    private Long employeeId;
    private long totalAppointmentsCompleted;
    private long totalAppointmentsCancelled;
    private double ratingAverage;
    private BigDecimal totalRevenueGenerated;
}
