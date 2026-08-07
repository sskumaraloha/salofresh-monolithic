package com.salofresh.service.giftcard;

import com.salofresh.dto.giftcard.GiftCardResponse;
import com.salofresh.dto.giftcard.PurchaseGiftCardRequest;
import com.salofresh.dto.giftcard.RedeemGiftCardResponse;
import com.salofresh.response.PagedResponse;
import org.springframework.data.domain.Pageable;

public interface GiftCardService {

    /**
     * Purchases a gift card. CASH is disallowed (gift cards must be prepaid). For WALLET the
     * debit happens synchronously and, on success, the gift card is issued immediately. For
     * gateway methods (UPI/CREDIT_CARD/DEBIT_CARD/RAZORPAY/STRIPE) a gateway order is created and
     * the gift card is issued only once {@link #confirm} succeeds.
     */
    GiftCardResponse purchase(Long userId, PurchaseGiftCardRequest request);

    /**
     * Confirms a gateway-method gift card purchase: verifies the gateway signature and, on
     * success, issues the gift card.
     */
    GiftCardResponse confirm(Long paymentId, String gatewayPaymentId, String gatewaySignature, Long userId);

    /**
     * Redeems a gift card in full: credits the entire remaining balance to the redeeming user's
     * wallet and marks the card REDEEMED. Partial redemption is not supported.
     */
    RedeemGiftCardResponse redeem(Long userId, String code);

    PagedResponse<GiftCardResponse> listPurchasedByMe(Long userId, Pageable pageable);

    /**
     * Public lookup of a gift card's balance/status by code; does not require authentication and
     * does not redeem the card.
     */
    GiftCardResponse checkBalance(String code);
}
