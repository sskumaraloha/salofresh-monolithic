package com.salofresh.controller.payout;

import com.salofresh.constant.AppConstants;
import com.salofresh.dto.payout.GeneratePayoutRequest;
import com.salofresh.dto.payout.ProcessPayoutRequest;
import com.salofresh.dto.payout.SalonPayoutResponse;
import com.salofresh.response.ApiResponse;
import com.salofresh.response.PagedResponse;
import com.salofresh.security.SecurityUtils;
import com.salofresh.service.payout.SalonPayoutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Platform-admin salon payout/settlement operations: generating a payout for a salon+period,
 * processing it, marking it paid, and viewing payouts across all salons.
 */
@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/admin/payouts")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
@Tag(name = "Admin - Salon Payouts", description = "Platform admin salon payout/settlement generation and processing")
public class AdminPayoutController {

    private final SalonPayoutService salonPayoutService;
    private final SecurityUtils securityUtils;

    @PostMapping("/generate")
    @Operation(summary = "Generate a settlement payout for a salon over a date range")
    public ResponseEntity<ApiResponse<SalonPayoutResponse>> generate(@Valid @RequestBody GeneratePayoutRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Payout generated successfully",
                salonPayoutService.generatePayout(securityUtils.getCurrentUserId(), request)));
    }

    @PutMapping("/{id}/process")
    @Operation(summary = "Move a PENDING payout to PROCESSED, assigning a payout reference")
    public ResponseEntity<ApiResponse<SalonPayoutResponse>> process(
            @PathVariable Long id, @RequestBody(required = false) ProcessPayoutRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Payout processed successfully",
                salonPayoutService.processPayout(securityUtils.getCurrentUserId(), id, request)));
    }

    @PutMapping("/{id}/pay")
    @Operation(summary = "Move a PROCESSED payout to PAID")
    public ResponseEntity<ApiResponse<SalonPayoutResponse>> markPaid(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Payout marked as paid successfully",
                salonPayoutService.markPaid(securityUtils.getCurrentUserId(), id)));
    }

    @GetMapping
    @Operation(summary = "List payouts across all salons")
    public ResponseEntity<ApiResponse<PagedResponse<SalonPayoutResponse>>> listAll(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Payouts fetched successfully",
                PagedResponse.from(salonPayoutService.listAll(securityUtils.getCurrentUserId(), pageable))));
    }
}
