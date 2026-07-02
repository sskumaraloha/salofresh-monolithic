package com.salofresh.dto.employee;

import com.salofresh.validation.ValidPhone;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
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
public class EmployeeCreateRequest {

    @NotBlank(message = "First name is required")
    private String firstName;

    private String lastName;

    @Email(message = "Email must be a valid email address")
    private String email;

    @ValidPhone
    private String phone;

    private String designation;

    private LocalDate joiningDate;

    @PositiveOrZero(message = "Salary must be zero or positive")
    private BigDecimal salary;

    private List<Long> serviceIds;
}
