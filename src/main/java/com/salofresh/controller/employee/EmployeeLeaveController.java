package com.salofresh.controller.employee;

import com.salofresh.constant.AppConstants;
import com.salofresh.dto.employee.LeaveDecisionRequest;
import com.salofresh.dto.employee.LeaveRequest;
import com.salofresh.dto.employee.LeaveResponse;
import com.salofresh.response.ApiResponse;
import com.salofresh.service.employee.EmployeeLeaveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

import java.util.List;

@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/salons/{salonId}/employees")
@RequiredArgsConstructor
@Tag(name = "Employee Leave", description = "Leave request and approval workflow for salon staff")
public class EmployeeLeaveController {

    private final EmployeeLeaveService employeeLeaveService;

    @PostMapping("/{employeeId}/leave")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Request leave for an employee (owner or the employee's own account)")
    public ResponseEntity<ApiResponse<LeaveResponse>> request(@PathVariable Long salonId,
                                                               @PathVariable Long employeeId,
                                                               @Valid @RequestBody LeaveRequest request) {
        LeaveResponse response = employeeLeaveService.request(salonId, employeeId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Leave requested successfully", response));
    }

    @GetMapping("/{employeeId}/leave")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List leave requests for an employee (owner or the employee's own account)")
    public ResponseEntity<ApiResponse<List<LeaveResponse>>> listByEmployee(@PathVariable Long salonId,
                                                                            @PathVariable Long employeeId) {
        List<LeaveResponse> response = employeeLeaveService.listByEmployee(salonId, employeeId);
        return ResponseEntity.ok(ApiResponse.success("Leave requests fetched successfully", response));
    }

    @PutMapping("/{employeeId}/leave/{leaveId}/decision")
    @PreAuthorize("hasRole('SALON_OWNER')")
    @Operation(summary = "Approve or reject an employee's leave request")
    public ResponseEntity<ApiResponse<LeaveResponse>> decide(@PathVariable Long salonId,
                                                              @PathVariable Long employeeId,
                                                              @PathVariable Long leaveId,
                                                              @Valid @RequestBody LeaveDecisionRequest request) {
        LeaveResponse response = employeeLeaveService.decide(salonId, employeeId, leaveId, request);
        return ResponseEntity.ok(ApiResponse.success("Leave decision recorded successfully", response));
    }

    @GetMapping("/leave/pending")
    @PreAuthorize("hasRole('SALON_OWNER')")
    @Operation(summary = "List all pending leave requests across the salon")
    public ResponseEntity<ApiResponse<List<LeaveResponse>>> listPendingForSalon(@PathVariable Long salonId) {
        List<LeaveResponse> response = employeeLeaveService.listPendingForSalon(salonId);
        return ResponseEntity.ok(ApiResponse.success("Pending leave requests fetched successfully", response));
    }
}
