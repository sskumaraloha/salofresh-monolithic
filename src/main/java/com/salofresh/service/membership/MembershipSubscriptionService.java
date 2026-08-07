package com.salofresh.service.membership;

import com.salofresh.dto.membership.MembershipSubscriptionResponse;
import com.salofresh.dto.membership.PurchaseMembershipRequest;
import com.salofresh.dto.membership.PurchaseMembershipResponse;
import com.salofresh.dto.payment.ConfirmPaymentRequest;
import com.salofresh.entity.MembershipSubscription;
import com.salofresh.response.PagedResponse;
import org.springframework.data.domain.Pageable;

public interface MembershipSubscriptionService {

    /**
     * Purchases a membership plan for the given user. Creates a new {@code Payment} (not linked
     * to any appointment) and processes it exactly like {@code PaymentServiceImpl}'s initiation
     * flow: CASH stays PENDING for manual confirmation, WALLET debits immediately, and gateway
     * methods (UPI/CREDIT_CARD/DEBIT_CARD/RAZORPAY/STRIPE) create a gateway order for client-side
     * confirmation via {@link #confirm}. The membership subscription itself is only created once
     * the payment actually succeeds.
     */
    PurchaseMembershipResponse purchase(Long userId, PurchaseMembershipRequest request);

    /**
     * Confirms/captures a previously initiated gateway payment for a membership purchase and, on
     * success, creates the resulting {@code MembershipSubscription}.
     */
    PurchaseMembershipResponse confirm(Long paymentId, ConfirmPaymentRequest request, Long userId);

    PagedResponse<MembershipSubscriptionResponse> listForUser(Long userId, Pageable pageable);

    /**
     * The current user's active, non-expired subscription usable at the given salon, or
     * {@code null} if they have none.
     */
    MembershipSubscriptionResponse getActiveSubscriptionForSalon(Long userId, Long salonId);

    /**
     * Finds the user's active subscription usable at the given salon (if any) and consumes one
     * session from it. Intended to be called from the booking flow so that it can opt into
     * membership consumption without depending on the rest of this module.
     *
     * @return the (now-updated) subscription that was consumed, or {@code null} if the user has
     * no active subscription usable at that salon
     * @throws com.salofresh.exception.BadRequestException if the subscription has no sessions left
     */
    MembershipSubscription consumeSessionIfApplicable(Long userId, Long salonId);
}
