package com.salofresh.controller.salon;

import com.salofresh.constant.AppConstants;
import com.salofresh.dto.salon.OwnerDashboardResponse;
import com.salofresh.response.ApiResponse;
import com.salofresh.service.salon.OwnerDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(AppConstants.API_BASE_PATH + "/owners/me/dashboard")
@Tag(name = "Owner Dashboard", description = "Aggregated metrics for the authenticated salon owner")
@PreAuthorize("hasRole('SALON_OWNER')")
public class OwnerDashboardController {

    private final OwnerDashboardService ownerDashboardService;

    @GetMapping
    @Operation(summary = "Get the dashboard summary for the authenticated salon owner")
    public ResponseEntity<ApiResponse<OwnerDashboardResponse>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.success("Dashboard fetched successfully", ownerDashboardService.getDashboard()));
    }
}
