package com.salofresh.controller.user;

import com.salofresh.constant.AppConstants;
import com.salofresh.dto.booking.CreateBookingRequest;
import com.salofresh.dto.employee.EmployeeResponse;
import com.salofresh.dto.user.SetPreferredStylistRequest;
import com.salofresh.entity.Employee;
import com.salofresh.mapper.employee.EmployeeMapper;
import com.salofresh.response.ApiResponse;
import com.salofresh.security.SecurityUtils;
import com.salofresh.service.user.PreferredStylistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/users/me/preferred-stylist")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Preferred Stylist", description = "Manage the authenticated customer's favorite/preferred stylist and quick-rebook against it")
public class PreferredStylistController {

    private final PreferredStylistService preferredStylistService;
    private final EmployeeMapper employeeMapper;
    private final SecurityUtils securityUtils;

    @PutMapping
    @Operation(summary = "Set the current customer's preferred stylist")
    public ResponseEntity<ApiResponse<Void>> setPreferredStylist(@Valid @RequestBody SetPreferredStylistRequest request) {
        preferredStylistService.setPreferredStylist(securityUtils.getCurrentUserId(), request.getEmployeeId());
        return ResponseEntity.ok(ApiResponse.success("Preferred stylist updated successfully"));
    }

    @GetMapping
    @Operation(summary = "Get the current customer's preferred stylist, if any")
    public ResponseEntity<ApiResponse<EmployeeResponse>> getPreferredStylist() {
        Employee employee = preferredStylistService.getPreferredStylist(securityUtils.getCurrentUserId());
        EmployeeResponse response = employee == null ? null : employeeMapper.toResponse(employee);
        return ResponseEntity.ok(ApiResponse.success("Preferred stylist fetched successfully", response));
    }

    @DeleteMapping
    @Operation(summary = "Clear the current customer's preferred stylist")
    public ResponseEntity<ApiResponse<Void>> clearPreferredStylist() {
        preferredStylistService.clearPreferredStylist(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success("Preferred stylist cleared successfully"));
    }

    @GetMapping("/quick-rebook/{appointmentId}")
    @Operation(summary = "Pre-fill a new booking request from a prior booking, defaulting the stylist to the "
            + "customer's preferred stylist when the prior booking didn't specify one")
    public ResponseEntity<ApiResponse<CreateBookingRequest>> quickRebook(@PathVariable Long appointmentId) {
        CreateBookingRequest template = preferredStylistService.quickRebook(securityUtils.getCurrentUserId(), appointmentId);
        return ResponseEntity.ok(ApiResponse.success("Quick-rebook template fetched successfully", template));
    }
}
