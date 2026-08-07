package com.salofresh.controller.payroll;

import com.salofresh.constant.AppConstants;
import com.salofresh.dto.payroll.PayrollRecordResponse;
import com.salofresh.response.ApiResponse;
import com.salofresh.response.PagedResponse;
import com.salofresh.security.SecurityUtils;
import com.salofresh.service.payroll.PayrollService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/employees/{employeeId}/payroll")
@RequiredArgsConstructor
@Tag(name = "Employee Payroll", description = "Employee self-service payroll history")
public class EmployeePayrollController {

    private final PayrollService payrollService;
    private final SecurityUtils securityUtils;

    @GetMapping
    @PreAuthorize("hasAnyRole('SALON_OWNER', 'EMPLOYEE')")
    @Operation(summary = "List payroll records for an employee (accessible by the employee themselves or the owning salon owner)")
    public ResponseEntity<ApiResponse<PagedResponse<PayrollRecordResponse>>> list(
            @PathVariable Long employeeId,
            @PageableDefault(size = 20) Pageable pageable) {
        PagedResponse<PayrollRecordResponse> response = payrollService.listForEmployee(
                securityUtils.getCurrentUserId(), employeeId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Payroll records fetched successfully", response));
    }
}
