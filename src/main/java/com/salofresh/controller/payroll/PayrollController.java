package com.salofresh.controller.payroll;

import com.salofresh.constant.AppConstants;
import com.salofresh.dto.payroll.GeneratePayrollRequest;
import com.salofresh.dto.payroll.PayrollRecordResponse;
import com.salofresh.response.ApiResponse;
import com.salofresh.response.PagedResponse;
import com.salofresh.security.SecurityUtils;
import com.salofresh.service.payroll.PayrollService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/salons/{salonId}/payroll")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SALON_OWNER')")
@Tag(name = "Payroll", description = "Staff payroll generation and lifecycle management (owner-only)")
public class PayrollController {

    private final PayrollService payrollService;
    private final SecurityUtils securityUtils;

    @PostMapping("/generate")
    @Operation(summary = "Generate a DRAFT payroll record for an employee for a given period, computing commission from completed appointments")
    public ResponseEntity<ApiResponse<PayrollRecordResponse>> generate(
            @PathVariable Long salonId,
            @Valid @RequestBody GeneratePayrollRequest request) {
        PayrollRecordResponse response = payrollService.generatePayroll(
                securityUtils.getCurrentUserId(), salonId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payroll record generated successfully", response));
    }

    @PutMapping("/{payrollRecordId}/finalize")
    @Operation(summary = "Finalize a DRAFT payroll record, locking it from further edits")
    public ResponseEntity<ApiResponse<PayrollRecordResponse>> finalize(
            @PathVariable Long salonId, @PathVariable Long payrollRecordId) {
        PayrollRecordResponse response = payrollService.finalizePayroll(
                securityUtils.getCurrentUserId(), payrollRecordId);
        return ResponseEntity.ok(ApiResponse.success("Payroll record finalized successfully", response));
    }

    @PutMapping("/{payrollRecordId}/pay")
    @Operation(summary = "Mark a FINALIZED payroll record as PAID")
    public ResponseEntity<ApiResponse<PayrollRecordResponse>> markPaid(
            @PathVariable Long salonId, @PathVariable Long payrollRecordId) {
        PayrollRecordResponse response = payrollService.markPaid(
                securityUtils.getCurrentUserId(), payrollRecordId);
        return ResponseEntity.ok(ApiResponse.success("Payroll record marked as paid successfully", response));
    }

    @GetMapping
    @Operation(summary = "List payroll records for a salon")
    public ResponseEntity<ApiResponse<PagedResponse<PayrollRecordResponse>>> list(
            @PathVariable Long salonId,
            @PageableDefault(size = 20) Pageable pageable) {
        PagedResponse<PayrollRecordResponse> response = payrollService.listForSalon(
                securityUtils.getCurrentUserId(), salonId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Payroll records fetched successfully", response));
    }

    @GetMapping("/{payrollRecordId}")
    @Operation(summary = "Get a payroll record by id")
    public ResponseEntity<ApiResponse<PayrollRecordResponse>> getById(
            @PathVariable Long salonId, @PathVariable Long payrollRecordId) {
        PayrollRecordResponse response = payrollService.getById(
                securityUtils.getCurrentUserId(), salonId, payrollRecordId);
        return ResponseEntity.ok(ApiResponse.success("Payroll record fetched successfully", response));
    }
}
