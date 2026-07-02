package com.salofresh.dto.employee;

import com.salofresh.common.enums.EmploymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeResponse {

    private Long id;
    private Long salonId;
    private String fullName;
    private String email;
    private String phone;
    private String designation;
    private LocalDate joiningDate;
    private BigDecimal salary;
    private EmploymentStatus employmentStatus;
    private String profileImageUrl;
    private double ratingAverage;
    private List<AssignedServiceSummary> assignedServices;
}
