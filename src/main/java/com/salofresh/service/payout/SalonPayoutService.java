package com.salofresh.service.payout;

import com.salofresh.dto.payout.GeneratePayoutRequest;
import com.salofresh.dto.payout.ProcessPayoutRequest;
import com.salofresh.dto.payout.SalonPayoutResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Computes and manages platform-owed salon payouts (settlement reports). Generating, processing
 * and marking a payout as paid are platform financial operations restricted to admins; salon
 * owners are limited to read-only visibility of their own salons' payout history.
 */
public interface SalonPayoutService {

    /**
     * Computes gross revenue for the salon over [periodStart, periodEnd] from completed
     * appointments, resolves the commission percentage, and creates a PENDING payout.
     * Admin-only.
     */
    SalonPayoutResponse generatePayout(Long adminUserId, GeneratePayoutRequest request);

    /**
     * Moves a PENDING payout to PROCESSED, assigning a payout reference (auto-generated if not
     * supplied). Admin-only.
     */
    SalonPayoutResponse processPayout(Long adminUserId, Long payoutId, ProcessPayoutRequest request);

    /**
     * Moves a PROCESSED payout to PAID, stamping {@code processedAt} if not already set.
     * Admin-only.
     */
    SalonPayoutResponse markPaid(Long adminUserId, Long payoutId);

    /**
     * Lists payouts across all salons. Admin-only.
     */
    Page<SalonPayoutResponse> listAll(Long adminUserId, Pageable pageable);

    /**
     * Lists payouts for a single salon. Admins may view any salon; a salon owner may only view
     * their own salon's payouts.
     */
    Page<SalonPayoutResponse> listForSalon(Long callerUserId, Long salonId, Pageable pageable);

    /**
     * Fetches a single payout by id. Same visibility rule as {@link #listForSalon}.
     */
    SalonPayoutResponse getById(Long callerUserId, Long payoutId);
}
