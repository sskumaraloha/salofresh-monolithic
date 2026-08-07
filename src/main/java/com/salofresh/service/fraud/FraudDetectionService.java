package com.salofresh.service.fraud;

import com.salofresh.common.enums.FraudAlertStatus;
import com.salofresh.dto.fraud.FraudAlertResponse;
import com.salofresh.dto.fraud.ResolveFraudAlertRequest;
import com.salofresh.response.PagedResponse;
import org.springframework.data.domain.Pageable;

public interface FraudDetectionService {

    /**
     * Scans for users with an unusually high count of failed payments in the last 24 hours and
     * raises a {@code REPEATED_FAILED_PAYMENTS} alert for each one (idempotent: skips users that
     * already have a matching open alert).
     */
    void detectRepeatedFailedPayments();

    /**
     * Scans for users who have posted an unusually high number of 5-star reviews in the last 24
     * hours ("review bombing") and raises a {@code SUSPICIOUS_REVIEW_PATTERN} alert for each one
     * (idempotent: skips users that already have a matching open alert).
     */
    void detectSuspiciousReviewPatterns();

    /**
     * Scans for referrers whose referral code has been used by an unusually high number of new
     * signups in the last 24 hours and raises a {@code REFERRAL_ABUSE} alert for each one
     * (idempotent: skips users that already have a matching open alert).
     */
    void detectReferralAbuse();

    PagedResponse<FraudAlertResponse> list(FraudAlertStatus status, Pageable pageable);

    FraudAlertResponse resolve(Long adminUserId, Long alertId, ResolveFraudAlertRequest request);
}
