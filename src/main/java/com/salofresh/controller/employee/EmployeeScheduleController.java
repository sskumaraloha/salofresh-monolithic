package com.salofresh.controller.employee;

import com.salofresh.constant.AppConstants;
import com.salofresh.dto.employee.ScheduleRequest;
import com.salofresh.dto.employee.ScheduleResponse;
import com.salofresh.response.ApiResponse;
import com.salofresh.service.employee.EmployeeScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/salons/{salonId}/employees/{employeeId}/schedule")
@RequiredArgsConstructor
@Tag(name = "Employee Schedule", description = "Weekly working-hours management for salon staff")
public class EmployeeScheduleController {

    private final EmployeeScheduleService employeeScheduleService;

    @GetMapping
    @PreAuthorize("permitAll()")
    @Operation(summary = "Get the weekly schedule of an employee (public, used during booking)")
    public ResponseEntity<ApiResponse<List<ScheduleResponse>>> getSchedule(@PathVariable Long salonId,
                                                                            @PathVariable Long employeeId) {
        List<ScheduleResponse> response = employeeScheduleService.getByEmployee(salonId, employeeId);
        return ResponseEntity.ok(ApiResponse.success("Employee schedule fetched successfully", response));
    }

    @PutMapping
    @PreAuthorize("hasRole('SALON_OWNER')")
    @Operation(summary = "Upsert the weekly schedule of an employee")
    public ResponseEntity<ApiResponse<List<ScheduleResponse>>> upsertSchedule(@PathVariable Long salonId,
                                                                               @PathVariable Long employeeId,
                                                                               @Valid @RequestBody ScheduleRequest request) {
        List<ScheduleResponse> response = employeeScheduleService.upsertWeeklySchedule(salonId, employeeId, request);
        return ResponseEntity.ok(ApiResponse.success("Employee schedule updated successfully", response));
    }
}
