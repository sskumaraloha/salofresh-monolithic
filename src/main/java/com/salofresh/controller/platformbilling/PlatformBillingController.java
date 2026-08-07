package com.salofresh.controller.platformbilling;

import com.salofresh.constant.AppConstants;
import com.salofresh.dto.platformbilling.PlatformPlanResponse;
import com.salofresh.dto.platformbilling.PlatformSubscriptionResponse;
import com.salofresh.dto.platformbilling.SubscribeToPlanRequest;
import com.salofresh.dto.platformbilling.SubscriptionPurchaseResult;
import com.salofresh.dto.payment.ConfirmPaymentRequest;
import com.salofresh.response.ApiResponse;
import com.salofresh.response.PagedResponse;
import com.salofresh.security.SecurityUtils;
import com.salofresh.service.platformbilling.PlatformBillingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Platform billing: SaloFresh charging salon owners a subscription fee to use the platform
 * itself. Distinct from a salon's own customer-facing membership plans (see the {@code
 * memberships} module), which are unrelated.
 */
@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/platform-billing")
@RequiredArgsConstructor
@Tag(name = "Platform Billing", description = "Platform subscription plans and billing for salon owners")
public class PlatformBillingController {

    private final PlatformBillingService platformBillingService;
    private final SecurityUtils securityUtils;

    @GetMapping("/plans")
    @PreAuthorize("permitAll()")
    @Operation(summary = "List the active platform plans available for salon owners to subscribe to")
    public ApiResponse<List<PlatformPlanResponse>> listPlans() {
        return ApiResponse.success("Platform plans fetched successfully", platformBillingService.listAvailablePlans());
    }

    @PostMapping("/subscribe")
    @PreAuthorize("hasRole('SALON_OWNER')")
    @Operation(summary = "Subscribe to a platform plan",
            description = "Creates a payment for the selected plan and, for gateway payment methods, returns "
                    + "the details needed to finish confirmation; CASH/WALLET payments are finalized immediately")
    public ApiResponse<SubscriptionPurchaseResult> subscribe(@Valid @RequestBody SubscribeToPlanRequest request) {
        SubscriptionPurchaseResult result = platformBillingService.subscribe(securityUtils.getCurrentUserId(), request);
        return ApiResponse.success("Platform plan subscription initiated", result);
    }

    @PostMapping("/{paymentId}/confirm")
    @PreAuthorize("hasRole('SALON_OWNER')")
    @Operation(summary = "Confirm/capture a platform plan subscription payment",
            description = "Verifies a gateway payment (or finalizes a CASH payment) and activates the platform subscription")
    public ApiResponse<SubscriptionPurchaseResult> confirm(@PathVariable Long paymentId,
                                                             @Valid @RequestBody ConfirmPaymentRequest request) {
        SubscriptionPurchaseResult result = platformBillingService.confirm(securityUtils.getCurrentUserId(), paymentId,
                request.getGatewayPaymentId(), request.getGatewaySignature());
        return ApiResponse.success("Platform plan subscription payment confirmed", result);
    }

    @GetMapping("/my-subscription")
    @PreAuthorize("hasRole('SALON_OWNER')")
    @Operation(summary = "Get the current owner's active platform subscription, if any")
    public ApiResponse<PlatformSubscriptionResponse> getMySubscription() {
        PlatformSubscriptionResponse response = platformBillingService.getMyActiveSubscription(securityUtils.getCurrentUserId());
        return ApiResponse.success("Active platform subscription fetched successfully", response);
    }

    @GetMapping("/my-subscription/history")
    @PreAuthorize("hasRole('SALON_OWNER')")
    @Operation(summary = "List the current owner's platform subscription purchase history")
    public ApiResponse<PagedResponse<PlatformSubscriptionResponse>> getMySubscriptionHistory(
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ApiResponse.success("Platform subscription history fetched successfully",
                platformBillingService.listMySubscriptionHistory(securityUtils.getCurrentUserId(), pageable));
    }
}
