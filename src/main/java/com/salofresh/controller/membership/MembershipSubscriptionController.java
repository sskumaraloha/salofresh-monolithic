package com.salofresh.controller.membership;

import com.salofresh.constant.AppConstants;
import com.salofresh.dto.membership.MembershipSubscriptionResponse;
import com.salofresh.dto.membership.PurchaseMembershipRequest;
import com.salofresh.dto.membership.PurchaseMembershipResponse;
import com.salofresh.dto.payment.ConfirmPaymentRequest;
import com.salofresh.response.ApiResponse;
import com.salofresh.response.PagedResponse;
import com.salofresh.security.SecurityUtils;
import com.salofresh.service.membership.MembershipSubscriptionService;
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

@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/memberships")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Memberships", description = "Purchasing and managing customer membership/subscription packages")
public class MembershipSubscriptionController {

    private final MembershipSubscriptionService membershipSubscriptionService;
    private final SecurityUtils securityUtils;

    @PostMapping("/purchase")
    @Operation(summary = "Purchase a membership plan",
            description = "Creates a payment for the selected plan and, for CASH/gateway methods, returns "
                    + "the details needed to finish confirmation; WALLET payments are finalized immediately")
    public ApiResponse<PurchaseMembershipResponse> purchase(@Valid @RequestBody PurchaseMembershipRequest request) {
        PurchaseMembershipResponse response = membershipSubscriptionService.purchase(securityUtils.getCurrentUserId(), request);
        return ApiResponse.success("Membership purchase initiated", response);
    }

    @PostMapping("/{paymentId}/confirm")
    @Operation(summary = "Confirm/capture a membership purchase payment",
            description = "Verifies a gateway payment (or finalizes a CASH payment) and activates the membership subscription")
    public ApiResponse<PurchaseMembershipResponse> confirm(@PathVariable Long paymentId,
                                                             @Valid @RequestBody ConfirmPaymentRequest request) {
        PurchaseMembershipResponse response = membershipSubscriptionService.confirm(paymentId, request, securityUtils.getCurrentUserId());
        return ApiResponse.success("Membership payment confirmed", response);
    }

    @GetMapping("/my")
    @Operation(summary = "List the current user's membership subscriptions")
    public ApiResponse<PagedResponse<MembershipSubscriptionResponse>> listMine(
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ApiResponse.success("Membership subscriptions fetched successfully",
                membershipSubscriptionService.listForUser(securityUtils.getCurrentUserId(), pageable));
    }

    @GetMapping("/salons/{salonId}/active")
    @Operation(summary = "Get the current user's active membership subscription usable at a salon, if any")
    public ApiResponse<MembershipSubscriptionResponse> getActiveForSalon(@PathVariable Long salonId) {
        MembershipSubscriptionResponse response =
                membershipSubscriptionService.getActiveSubscriptionForSalon(securityUtils.getCurrentUserId(), salonId);
        return ApiResponse.success("Active membership subscription fetched successfully", response);
    }
}
