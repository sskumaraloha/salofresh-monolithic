package com.salofresh.service.impl.fraud;

import com.salofresh.common.enums.FraudAlertSeverity;
import com.salofresh.common.enums.FraudAlertStatus;
import com.salofresh.common.enums.FraudAlertType;
import com.salofresh.common.enums.PaymentStatus;
import com.salofresh.dto.fraud.FraudAlertResponse;
import com.salofresh.dto.fraud.ResolveFraudAlertRequest;
import com.salofresh.entity.FraudAlert;
import com.salofresh.entity.Payment;
import com.salofresh.entity.Review;
import com.salofresh.entity.User;
import com.salofresh.exception.BadRequestException;
import com.salofresh.exception.ResourceNotFoundException;
import com.salofresh.repository.FraudAlertRepository;
import com.salofresh.repository.PaymentRepository;
import com.salofresh.repository.ReviewRepository;
import com.salofresh.repository.UserRepository;
import com.salofresh.response.PagedResponse;
import com.salofresh.service.fraud.FraudDetectionService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FraudDetectionServiceImpl implements FraudDetectionService {

    private static final Logger log = LoggerFactory.getLogger(FraudDetectionServiceImpl.class);

    private static final int FAILED_PAYMENTS_THRESHOLD = 3;
    private static final int REVIEW_BOMBING_THRESHOLD = 5;
    private static final int REVIEW_BOMBING_RATING = 5;
    private static final int REFERRAL_ABUSE_THRESHOLD = 10;

    private final FraudAlertRepository fraudAlertRepository;
    private final PaymentRepository paymentRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void detectRepeatedFailedPayments() {
        Instant since = Instant.now().minus(24, ChronoUnit.HOURS);
        List<Payment> recentFailures = paymentRepository.findAllByPaymentStatusAndCreatedAtAfter(PaymentStatus.FAILED, since);

        Map<Long, List<Payment>> byUser = recentFailures.stream()
                .filter(payment -> payment.getUser() != null)
                .collect(Collectors.groupingBy(payment -> payment.getUser().getId()));

        for (Map.Entry<Long, List<Payment>> entry : byUser.entrySet()) {
            Long userId = entry.getKey();
            int count = entry.getValue().size();
            if (count < FAILED_PAYMENTS_THRESHOLD) {
                continue;
            }
            if (fraudAlertRepository.existsByRelatedUserIdAndTypeAndStatus(
                    userId, FraudAlertType.REPEATED_FAILED_PAYMENTS, FraudAlertStatus.OPEN)) {
                continue;
            }
            User user = entry.getValue().get(0).getUser();
            FraudAlert alert = FraudAlert.builder()
                    .type(FraudAlertType.REPEATED_FAILED_PAYMENTS)
                    .relatedUser(user)
                    .relatedEntityType("User")
                    .relatedEntityId(String.valueOf(userId))
                    .description("User %d has had %d failed payments in the last 24 hours".formatted(userId, count))
                    .severity(FraudAlertSeverity.HIGH)
                    .status(FraudAlertStatus.OPEN)
                    .detectedAt(Instant.now())
                    .build();
            fraudAlertRepository.save(alert);
        }
    }

    @Override
    @Transactional
    public void detectSuspiciousReviewPatterns() {
        Instant since = Instant.now().minus(24, ChronoUnit.HOURS);
        List<Review> recentTopRatedReviews =
                reviewRepository.findAllBySalonRatingAndCreatedAtAfterAndDeletedFalse(REVIEW_BOMBING_RATING, since);

        Map<Long, List<Review>> byCustomer = recentTopRatedReviews.stream()
                .filter(review -> review.getCustomer() != null)
                .collect(Collectors.groupingBy(review -> review.getCustomer().getId()));

        for (Map.Entry<Long, List<Review>> entry : byCustomer.entrySet()) {
            Long customerId = entry.getKey();
            int count = entry.getValue().size();
            if (count < REVIEW_BOMBING_THRESHOLD) {
                continue;
            }
            if (fraudAlertRepository.existsByRelatedUserIdAndTypeAndStatus(
                    customerId, FraudAlertType.SUSPICIOUS_REVIEW_PATTERN, FraudAlertStatus.OPEN)) {
                continue;
            }
            User customer = entry.getValue().get(0).getCustomer();
            FraudAlert alert = FraudAlert.builder()
                    .type(FraudAlertType.SUSPICIOUS_REVIEW_PATTERN)
                    .relatedUser(customer)
                    .relatedEntityType("User")
                    .relatedEntityId(String.valueOf(customerId))
                    .description("User %d has posted %d five-star reviews in the last 24 hours"
                            .formatted(customerId, count))
                    .severity(FraudAlertSeverity.MEDIUM)
                    .status(FraudAlertStatus.OPEN)
                    .detectedAt(Instant.now())
                    .build();
            fraudAlertRepository.save(alert);
        }
    }

    @Override
    @Transactional
    public void detectReferralAbuse() {
        Instant since = Instant.now().minus(24, ChronoUnit.HOURS);
        List<User> recentlyReferred = userRepository.findAllByReferredByCodeIsNotNullAndCreatedAtAfter(since);

        Map<String, Long> countByReferrerCode = recentlyReferred.stream()
                .collect(Collectors.groupingBy(User::getReferredByCode, Collectors.counting()));

        for (Map.Entry<String, Long> entry : countByReferrerCode.entrySet()) {
            String referrerCode = entry.getKey();
            long count = entry.getValue();
            if (count <= REFERRAL_ABUSE_THRESHOLD) {
                continue;
            }
            userRepository.findByReferralCode(referrerCode).ifPresent(referrer -> {
                Long referrerId = referrer.getId();
                if (fraudAlertRepository.existsByRelatedUserIdAndTypeAndStatus(
                        referrerId, FraudAlertType.REFERRAL_ABUSE, FraudAlertStatus.OPEN)) {
                    return;
                }
                FraudAlert alert = FraudAlert.builder()
                        .type(FraudAlertType.REFERRAL_ABUSE)
                        .relatedUser(referrer)
                        .relatedEntityType("User")
                        .relatedEntityId(String.valueOf(referrerId))
                        .description("User %d has referred %d new users in the last 24 hours"
                                .formatted(referrerId, count))
                        .severity(FraudAlertSeverity.HIGH)
                        .status(FraudAlertStatus.OPEN)
                        .detectedAt(Instant.now())
                        .build();
                fraudAlertRepository.save(alert);
            });
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<FraudAlertResponse> list(FraudAlertStatus status, Pageable pageable) {
        Page<FraudAlert> page = status != null
                ? fraudAlertRepository.findAllByStatusOrderByDetectedAtDesc(status, pageable)
                : fraudAlertRepository.findAllByOrderByDetectedAtDesc(pageable);
        return PagedResponse.from(page, page.getContent().stream().map(this::toResponse).toList());
    }

    @Override
    @Transactional
    public FraudAlertResponse resolve(Long adminUserId, Long alertId, ResolveFraudAlertRequest request) {
        if (request.getStatus() != FraudAlertStatus.RESOLVED && request.getStatus() != FraudAlertStatus.DISMISSED) {
            throw new BadRequestException("Status must be RESOLVED or DISMISSED");
        }

        FraudAlert alert = fraudAlertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("FraudAlert", "id", alertId));

        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", adminUserId));

        alert.setStatus(request.getStatus());
        alert.setResolvedBy(admin);
        alert.setResolvedAt(Instant.now());
        alert.setResolutionNote(request.getResolutionNote());

        FraudAlert saved = fraudAlertRepository.save(alert);
        return toResponse(saved);
    }

    private FraudAlertResponse toResponse(FraudAlert alert) {
        User relatedUser = alert.getRelatedUser();
        return FraudAlertResponse.builder()
                .id(alert.getId())
                .type(alert.getType())
                .relatedUserId(relatedUser != null ? relatedUser.getId() : null)
                .relatedUserName(relatedUser != null ? relatedUser.getFullName() : null)
                .relatedEntityType(alert.getRelatedEntityType())
                .relatedEntityId(alert.getRelatedEntityId())
                .description(alert.getDescription())
                .severity(alert.getSeverity())
                .status(alert.getStatus())
                .detectedAt(alert.getDetectedAt())
                .resolvedById(alert.getResolvedBy() != null ? alert.getResolvedBy().getId() : null)
                .resolvedAt(alert.getResolvedAt())
                .resolutionNote(alert.getResolutionNote())
                .build();
    }
}
