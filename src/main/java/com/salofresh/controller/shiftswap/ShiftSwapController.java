package com.salofresh.controller.shiftswap;

import com.salofresh.common.enums.ShiftSwapStatus;
import com.salofresh.constant.AppConstants;
import com.salofresh.dto.shiftswap.CreateShiftSwapRequest;
import com.salofresh.dto.shiftswap.ShiftSwapDecisionRequest;
import com.salofresh.dto.shiftswap.ShiftSwapResponse;
import com.salofresh.response.ApiResponse;
import com.salofresh.response.PagedResponse;
import com.salofresh.security.SecurityUtils;
import com.salofresh.service.shiftswap.ShiftSwapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Shift-swap request/approval workflow for salon staff.
 *
 * <p>NON-GOAL: approving a request here does not automatically rewrite {@code EmployeeSchedule}
 * rows — it is a coordination and visibility tool, not an automatic rota rewrite.</p>
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Shift Swap Requests", description = "Staff-initiated shift-swap request and approval workflow")
public class ShiftSwapController {

    private final ShiftSwapService shiftSwapService;
    private final SecurityUtils securityUtils;

    @PostMapping(AppConstants.API_BASE_PATH + "/salons/{salonId}/employees/{employeeId}/shift-swaps")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Request a shift swap for an employee (the employee's own account or the salon owner)")
    public ResponseEntity<ApiResponse<ShiftSwapResponse>> create(@PathVariable Long salonId,
                                                                   @PathVariable Long employeeId,
                                                                   @Valid @RequestBody CreateShiftSwapRequest request) {
        ShiftSwapResponse response = shiftSwapService.create(securityUtils.getCurrentUserId(), salonId, employeeId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Shift-swap request created successfully", response));
    }

    @GetMapping(AppConstants.API_BASE_PATH + "/salons/{salonId}/employees/{employeeId}/shift-swaps")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List shift-swap requests raised by an employee (the employee's own account or the salon owner)")
    public ResponseEntity<ApiResponse<PagedResponse<ShiftSwapResponse>>> listForEmployee(
            @PathVariable Long salonId,
            @PathVariable Long employeeId,
            @PageableDefault(size = 20) Pageable pageable) {
        PagedResponse<ShiftSwapResponse> response =
                shiftSwapService.listForEmployee(securityUtils.getCurrentUserId(), salonId, employeeId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Shift-swap requests fetched successfully", response));
    }

    @DeleteMapping(AppConstants.API_BASE_PATH + "/salons/{salonId}/employees/{employeeId}/shift-swaps/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cancel a still-pending shift-swap request (the requesting employee's own account or the salon owner)")
    public ResponseEntity<ApiResponse<ShiftSwapResponse>> cancel(@PathVariable Long salonId,
                                                                   @PathVariable Long employeeId,
                                                                   @PathVariable Long id) {
        ShiftSwapResponse response = shiftSwapService.cancel(securityUtils.getCurrentUserId(), salonId, id);
        return ResponseEntity.ok(ApiResponse.success("Shift-swap request cancelled successfully", response));
    }

    @GetMapping(AppConstants.API_BASE_PATH + "/salons/{salonId}/shift-swaps")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List all shift-swap requests for the salon, optionally filtered by status (salon owner only)")
    public ResponseEntity<ApiResponse<PagedResponse<ShiftSwapResponse>>> listForSalon(
            @PathVariable Long salonId,
            @RequestParam(required = false) ShiftSwapStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        PagedResponse<ShiftSwapResponse> response =
                shiftSwapService.listForSalon(securityUtils.getCurrentUserId(), salonId, status, pageable);
        return ResponseEntity.ok(ApiResponse.success("Shift-swap requests fetched successfully", response));
    }

    @PutMapping(AppConstants.API_BASE_PATH + "/salons/{salonId}/shift-swaps/{id}/decision")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Approve or reject a pending shift-swap request (salon owner only)")
    public ResponseEntity<ApiResponse<ShiftSwapResponse>> decide(@PathVariable Long salonId,
                                                                    @PathVariable Long id,
                                                                    @Valid @RequestBody ShiftSwapDecisionRequest request) {
        ShiftSwapResponse response = shiftSwapService.decide(securityUtils.getCurrentUserId(), salonId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Shift-swap decision recorded successfully", response));
    }
}
