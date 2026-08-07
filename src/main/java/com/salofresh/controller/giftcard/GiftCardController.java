package com.salofresh.controller.giftcard;

import com.salofresh.constant.AppConstants;
import com.salofresh.dto.giftcard.GiftCardResponse;
import com.salofresh.dto.giftcard.PurchaseGiftCardRequest;
import com.salofresh.dto.giftcard.RedeemGiftCardRequest;
import com.salofresh.dto.giftcard.RedeemGiftCardResponse;
import com.salofresh.dto.payment.ConfirmPaymentRequest;
import com.salofresh.response.ApiResponse;
import com.salofresh.response.PagedResponse;
import com.salofresh.security.SecurityUtils;
import com.salofresh.service.giftcard.GiftCardService;
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
@RequestMapping(AppConstants.API_BASE_PATH + "/gift-cards")
@RequiredArgsConstructor
@Tag(name = "Gift Cards", description = "Purchase, redeem and look up SaloFresh gift cards")
public class GiftCardController {

    private final GiftCardService giftCardService;
    private final SecurityUtils securityUtils;

    @PostMapping("/purchase")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Purchase a gift card; WALLET is settled immediately, gateway methods require a follow-up confirm call")
    public ApiResponse<GiftCardResponse> purchase(@Valid @RequestBody PurchaseGiftCardRequest request) {
        GiftCardResponse response = giftCardService.purchase(securityUtils.getCurrentUserId(), request);
        return ApiResponse.success("Gift card purchase initiated", response);
    }

    @PostMapping("/{paymentId}/confirm")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Confirm a gateway-method gift card purchase and issue the gift card")
    public ApiResponse<GiftCardResponse> confirm(@PathVariable Long paymentId,
                                                  @Valid @RequestBody ConfirmPaymentRequest request) {
        GiftCardResponse response = giftCardService.confirm(paymentId, request.getGatewayPaymentId(),
                request.getGatewaySignature(), securityUtils.getCurrentUserId());
        return ApiResponse.success("Gift card purchase confirmed", response);
    }

    @PostMapping("/redeem")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Redeem a gift card in full into the current user's wallet")
    public ApiResponse<RedeemGiftCardResponse> redeem(@Valid @RequestBody RedeemGiftCardRequest request) {
        RedeemGiftCardResponse response = giftCardService.redeem(securityUtils.getCurrentUserId(), request.getCode());
        return ApiResponse.success("Gift card redeemed", response);
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List gift cards purchased by the current user")
    public ApiResponse<PagedResponse<GiftCardResponse>> listMine(
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        PagedResponse<GiftCardResponse> response = giftCardService.listPurchasedByMe(securityUtils.getCurrentUserId(), pageable);
        return ApiResponse.success("Gift cards fetched", response);
    }

    @GetMapping("/{code}/balance")
    @Operation(summary = "Publicly check a gift card's balance and status by code")
    public ApiResponse<GiftCardResponse> checkBalance(@PathVariable String code) {
        return ApiResponse.success("Gift card balance fetched", giftCardService.checkBalance(code));
    }
}
