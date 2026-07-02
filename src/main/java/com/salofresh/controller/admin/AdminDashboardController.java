package com.salofresh.controller.admin;

import com.salofresh.constant.AppConstants;
import com.salofresh.dto.admin.PlatformDashboardResponse;
import com.salofresh.response.ApiResponse;
import com.salofresh.service.admin.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
@Tag(name = "Admin - Dashboard", description = "Platform-wide operational dashboard summary")
public class AdminDashboardController {

    private final AnalyticsService analyticsService;

    @GetMapping("/summary")
    @Operation(summary = "Get platform dashboard summary metrics")
    public ResponseEntity<ApiResponse<PlatformDashboardResponse>> summary() {
        return ResponseEntity.ok(ApiResponse.success("Dashboard summary fetched successfully",
                analyticsService.getPlatformDashboard()));
    }
}
