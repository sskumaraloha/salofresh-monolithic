package com.salofresh.service.platformbilling;

import com.salofresh.dto.platformbilling.PlatformPlanResponse;
import com.salofresh.dto.platformbilling.PlatformSubscriptionResponse;
import com.salofresh.dto.platformbilling.SubscribeToPlanRequest;
import com.salofresh.dto.platformbilling.SubscriptionPurchaseResult;
import com.salofresh.response.PagedResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Platform-level billing: SaloFresh charging salon owners a subscription fee to use the
 * platform itself. Distinct from a salon's own customer-facing membership plans
 * ({@code com.salofresh.service.membership}), which are unrelated and untouched by this service.
 */
public interface PlatformBillingService {

    /**
     * Lists the currently active platform plans available for salon owners to subscribe to.
     */
    List<PlatformPlanResponse> listAvailablePlans();

    /**
     * Initiates a purchase of a platform plan for the given owner. Creates a {@code Payment} and
     * routes it by payment method (CASH/WALLET finalize immediately, gateway methods create a
     * gateway order and require a subsequent {@link #confirm} call). Any prior ACTIVE subscription
     * held by the owner is cancelled so at most one platform subscription is active at a time.
     */
    SubscriptionPurchaseResult subscribe(Long ownerUserId, SubscribeToPlanRequest request);

    /**
     * Confirms/captures a gateway-initiated platform plan purchase payment (or finalizes a CASH
     * payment), activating the linked platform subscription on success.
     */
    SubscriptionPurchaseResult confirm(Long ownerUserId, Long paymentId, String gatewayPaymentId, String gatewaySignature);

    /**
     * Returns the owner's currently usable (ACTIVE and paid) platform subscription, or
     * {@code null} if none exists.
     */
    PlatformSubscriptionResponse getMyActiveSubscription(Long ownerUserId);

    /**
     * Lists the owner's platform subscription purchase history, most recent first.
     */
    PagedResponse<PlatformSubscriptionResponse> listMySubscriptionHistory(Long ownerUserId, Pageable pageable);

    /**
     * Known integration point for the salon module: returns whether the owner is currently
     * allowed to create another salon, based on their active platform subscription's
     * {@code maxSalons} limit (or unlimited) versus their current non-deleted salon count. This
     * method is NOT wired into the salon-creation flow (out of scope here) — a future change to
     * the salon module should call this before allowing salon creation.
     */
    boolean canCreateAnotherSalon(Long ownerUserId);
}
