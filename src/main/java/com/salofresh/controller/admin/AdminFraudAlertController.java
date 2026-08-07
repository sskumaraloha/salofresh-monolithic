package com.salofresh.controller.admin;

import com.salofresh.common.enums.FraudAlertStatus;
import com.salofresh.constant.AppConstants;
import com.salofresh.dto.fraud.FraudAlertResponse;
import com.salofresh.dto.fraud.ResolveFraudAlertRequest;
import com.salofresh.response.ApiResponse;
import com.salofresh.response.PagedResponse;
import com.salofresh.security.SecurityUtils;
import com.salofresh.service.fraud.FraudDetectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/admin/fraud-alerts")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
@Tag(name = "Admin - Fraud Alerts", description = "Platform admin review and resolution of automatically-detected fraud/anomaly alerts")
public class AdminFraudAlertController {

    private final FraudDetectionService fraudDetectionService;
    private final SecurityUtils securityUtils;

    @GetMapping
    @Operation(summary = "List fraud alerts, optionally filtered by status")
    public ResponseEntity<ApiResponse<PagedResponse<FraudAlertResponse>>> list(
            @RequestParam(required = false) FraudAlertStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Fraud alerts fetched successfully",
                fraudDetectionService.list(status, pageable)));
    }

    @PutMapping("/{id}/resolve")
    @Operation(summary = "Resolve or dismiss a fraud alert")
    public ResponseEntity<ApiResponse<FraudAlertResponse>> resolve(
            @PathVariable Long id, @Valid @RequestBody ResolveFraudAlertRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Fraud alert updated successfully",
                fraudDetectionService.resolve(securityUtils.getCurrentUserId(), id, request)));
    }
}
