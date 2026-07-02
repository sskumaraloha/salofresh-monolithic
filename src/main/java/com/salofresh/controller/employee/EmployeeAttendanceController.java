package com.salofresh.controller.employee;

import com.salofresh.constant.AppConstants;
import com.salofresh.dto.employee.AttendanceRequest;
import com.salofresh.dto.employee.AttendanceResponse;
import com.salofresh.response.ApiResponse;
import com.salofresh.service.employee.EmployeeAttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/salons/{salonId}/employees")
@RequiredArgsConstructor
@Tag(name = "Employee Attendance", description = "Daily attendance recording and reporting for salon staff (owner-only)")
public class EmployeeAttendanceController {

    private final EmployeeAttendanceService employeeAttendanceService;

    @PostMapping("/{employeeId}/attendance")
    @PreAuthorize("hasRole('SALON_OWNER')")
    @Operation(summary = "Record check-in/check-out attendance for an employee on a given date")
    public ResponseEntity<ApiResponse<AttendanceResponse>> record(@PathVariable Long salonId,
                                                                   @PathVariable Long employeeId,
                                                                   @Valid @RequestBody AttendanceRequest request) {
        AttendanceResponse response = employeeAttendanceService.record(salonId, employeeId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Attendance recorded successfully", response));
    }

    @GetMapping("/{employeeId}/attendance")
    @PreAuthorize("hasRole('SALON_OWNER')")
    @Operation(summary = "List attendance records for an employee within a date range")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> listForEmployee(
            @PathVariable Long salonId,
            @PathVariable Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<AttendanceResponse> response =
                employeeAttendanceService.listForEmployee(salonId, employeeId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Attendance records fetched successfully", response));
    }

    @GetMapping("/{employeeId}/attendance/summary")
    @PreAuthorize("hasRole('SALON_OWNER')")
    @Operation(summary = "Get the attendance summary for an employee on a specific date")
    public ResponseEntity<ApiResponse<AttendanceResponse>> dailySummary(
            @PathVariable Long salonId,
            @PathVariable Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        AttendanceResponse response = employeeAttendanceService.dailySummary(salonId, employeeId, date);
        return ResponseEntity.ok(ApiResponse.success("Attendance summary fetched successfully", response));
    }
}
