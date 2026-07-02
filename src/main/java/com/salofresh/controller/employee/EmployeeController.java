package com.salofresh.controller.employee;

import com.salofresh.common.enums.EmploymentStatus;
import com.salofresh.constant.AppConstants;
import com.salofresh.dto.employee.AssignServicesRequest;
import com.salofresh.dto.employee.EmployeeCreateRequest;
import com.salofresh.dto.employee.EmployeePerformanceResponse;
import com.salofresh.dto.employee.EmployeeResponse;
import com.salofresh.dto.employee.EmployeeUpdateRequest;
import com.salofresh.response.ApiResponse;
import com.salofresh.response.PagedResponse;
import com.salofresh.service.employee.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/salons/{salonId}/employees")
@RequiredArgsConstructor
@Tag(name = "Employees", description = "Salon staff management")
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping
    @PreAuthorize("hasRole('SALON_OWNER')")
    @Operation(summary = "Add a new employee to a salon")
    public ResponseEntity<ApiResponse<EmployeeResponse>> create(@PathVariable Long salonId,
                                                                 @Valid @RequestBody EmployeeCreateRequest request) {
        EmployeeResponse response = employeeService.create(salonId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Employee created successfully", response));
    }

    @PutMapping("/{employeeId}")
    @PreAuthorize("hasRole('SALON_OWNER')")
    @Operation(summary = "Update an existing employee")
    public ResponseEntity<ApiResponse<EmployeeResponse>> update(@PathVariable Long salonId,
                                                                 @PathVariable Long employeeId,
                                                                 @Valid @RequestBody EmployeeUpdateRequest request) {
        EmployeeResponse response = employeeService.update(salonId, employeeId, request);
        return ResponseEntity.ok(ApiResponse.success("Employee updated successfully", response));
    }

    @DeleteMapping("/{employeeId}")
    @PreAuthorize("hasRole('SALON_OWNER')")
    @Operation(summary = "Soft delete an employee")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long salonId, @PathVariable Long employeeId) {
        employeeService.delete(salonId, employeeId);
        return ResponseEntity.ok(ApiResponse.success("Employee deleted successfully"));
    }

    @GetMapping
    @PreAuthorize("permitAll()")
    @Operation(summary = "List employees for a salon (public, used during booking)")
    public ResponseEntity<ApiResponse<PagedResponse<EmployeeResponse>>> list(
            @PathVariable Long salonId,
            @RequestParam(required = false) EmploymentStatus status,
            @RequestParam(required = false) Long serviceId,
            @PageableDefault(size = 20) Pageable pageable) {
        PagedResponse<EmployeeResponse> response = employeeService.listBySalon(salonId, status, serviceId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Employees fetched successfully", response));
    }

    @GetMapping("/{employeeId}")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Get employee detail (public, used during booking)")
    public ResponseEntity<ApiResponse<EmployeeResponse>> getById(@PathVariable Long salonId,
                                                                  @PathVariable Long employeeId) {
        EmployeeResponse response = employeeService.getById(salonId, employeeId);
        return ResponseEntity.ok(ApiResponse.success("Employee fetched successfully", response));
    }

    @PostMapping("/{employeeId}/services")
    @PreAuthorize("hasRole('SALON_OWNER')")
    @Operation(summary = "Assign services offered by the salon to an employee")
    public ResponseEntity<ApiResponse<EmployeeResponse>> assignServices(
            @PathVariable Long salonId,
            @PathVariable Long employeeId,
            @Valid @RequestBody AssignServicesRequest request) {
        EmployeeResponse response = employeeService.assignServices(salonId, employeeId, request.getServiceIds());
        return ResponseEntity.ok(ApiResponse.success("Services assigned successfully", response));
    }

    @PostMapping(value = "/{employeeId}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SALON_OWNER')")
    @Operation(summary = "Upload or replace an employee's profile photo")
    public ResponseEntity<ApiResponse<EmployeeResponse>> uploadPhoto(@PathVariable Long salonId,
                                                                      @PathVariable Long employeeId,
                                                                      @RequestPart("file") MultipartFile file) {
        EmployeeResponse response = employeeService.uploadPhoto(salonId, employeeId, file);
        return ResponseEntity.ok(ApiResponse.success("Employee photo uploaded successfully", response));
    }

    @GetMapping("/{employeeId}/performance")
    @PreAuthorize("hasRole('SALON_OWNER')")
    @Operation(summary = "Get performance statistics for an employee")
    public ResponseEntity<ApiResponse<EmployeePerformanceResponse>> getPerformance(@PathVariable Long salonId,
                                                                                    @PathVariable Long employeeId) {
        EmployeePerformanceResponse response = employeeService.getPerformance(salonId, employeeId);
        return ResponseEntity.ok(ApiResponse.success("Employee performance fetched successfully", response));
    }
}
