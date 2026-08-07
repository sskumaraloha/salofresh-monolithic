package com.salofresh.controller.payout;

import com.salofresh.constant.AppConstants;
import com.salofresh.dto.payout.SalonPayoutResponse;
import com.salofresh.response.ApiResponse;
import com.salofresh.response.PagedResponse;
import com.salofresh.security.SecurityUtils;
import com.salofresh.service.payout.SalonPayoutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only payout/settlement visibility scoped to a salon: platform admins may view any
 * salon's payout history, while a salon owner may only view their own salon's payouts (enforced
 * in {@link SalonPayoutService}).
 */
@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/salons/{salonId}/payouts")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Salon Payouts", description = "Salon owner / admin view of a salon's payout (settlement) history")
public class SalonPayoutController {

    private final SalonPayoutService salonPayoutService;
    private final SecurityUtils securityUtils;

    @GetMapping
    @Operation(summary = "List payouts for a salon (own salon for owners, any salon for admins)")
    public ResponseEntity<ApiResponse<PagedResponse<SalonPayoutResponse>>> list(
            @PathVariable Long salonId, @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Payouts fetched successfully",
                PagedResponse.from(salonPayoutService.listForSalon(securityUtils.getCurrentUserId(), salonId, pageable))));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single payout by id (own salon for owners, any salon for admins)")
    public ResponseEntity<ApiResponse<SalonPayoutResponse>> getById(
            @PathVariable Long salonId, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Payout fetched successfully",
                salonPayoutService.getById(securityUtils.getCurrentUserId(), id)));
    }
}
