package com.salofresh.service.impl.platformbilling;

import com.salofresh.common.enums.BillingCycle;
import com.salofresh.common.enums.MembershipStatus;
import com.salofresh.common.enums.PaymentMethod;
import com.salofresh.common.enums.PaymentStatus;
import com.salofresh.common.enums.WalletTransactionSource;
import com.salofresh.common.enums.WalletTransactionType;
import com.salofresh.constant.AppConstants;
import com.salofresh.dto.platformbilling.PlatformSubscriptionResponse;
import com.salofresh.dto.platformbilling.SubscribeToPlanRequest;
import com.salofresh.dto.platformbilling.SubscriptionPurchaseResult;
import com.salofresh.entity.Payment;
import com.salofresh.entity.PlatformPlan;
import com.salofresh.entity.PlatformSubscription;
import com.salofresh.entity.SalonOwner;
import com.salofresh.entity.Transaction;
import com.salofresh.exception.BadRequestException;
import com.salofresh.exception.ForbiddenException;
import com.salofresh.exception.ResourceNotFoundException;
import com.salofresh.mapper.platformbilling.PlatformBillingMapper;
import com.salofresh.repository.PaymentRepository;
import com.salofresh.repository.PlatformPlanRepository;
import com.salofresh.repository.PlatformSubscriptionRepository;
import com.salofresh.repository.SalonOwnerRepository;
import com.salofresh.repository.SalonRepository;
import com.salofresh.repository.TransactionRepository;
import com.salofresh.response.PagedResponse;
import com.salofresh.security.SecurityUtils;
import com.salofresh.security.UserPrincipal;
import com.salofresh.service.impl.payment.PaymentGatewayFactory;
import com.salofresh.service.payment.PaymentGateway;
import com.salofresh.service.platformbilling.PlatformBillingService;
import com.salofresh.util.RandomCodeGenerator;
import com.salofresh.wallet.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Handles platform billing/subscriptions: SaloFresh charging salon owners a recurring fee to use
 * the platform itself. This is intentionally separate from
 * {@code MembershipSubscriptionServiceImpl}, which handles a completely different domain (a
 * salon's customer-facing membership packages) even though the payment plumbing looks similar.
 *
 * <p>Because {@code Payment} does not (and per scope, must not) carry a reference to the plan
 * being purchased, the resulting {@link PlatformSubscription} is created eagerly, in the same
 * call that creates the {@code Payment}, always linked to it via {@code payment_id}. This lets
 * {@link #confirm} locate it later purely from {@code paymentId} (via
 * {@link PlatformSubscriptionRepository#findByPaymentId}), mirroring
 * {@code MembershipSubscriptionServiceImpl}'s exact reasoning.
 *
 * <p><b>Assumption on "no overlapping active subscriptions":</b> because
 * {@link PlatformSubscriptionRepository} only exposes a single-result
 * {@code findFirstBySalonOwnerIdAndStatusOrderByEndDateDesc} query (no plural/list lookup, per
 * scope restrictions on touching repositories), any prior ACTIVE subscription is cancelled
 * eagerly at {@link #subscribe} time — before the new payment resolves — rather than deferred
 * until the new payment succeeds. This keeps "at most one ACTIVE row per owner" an invariant that
 * holds at all times and avoids ambiguity when the new subscription's later end date would
 * otherwise make it indistinguishable from the "prior" one in that single-result query. The
 * trade-off: if a gateway payment for the new plan subsequently fails, the owner is left without
 * an active subscription until they retry (their old one is not restored) — same lapse behavior
 * as most real-world subscription billing systems.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PlatformBillingServiceImpl implements PlatformBillingService {

    private static final Set<PaymentMethod> GATEWAY_METHODS = Set.of(
            PaymentMethod.UPI, PaymentMethod.CREDIT_CARD, PaymentMethod.DEBIT_CARD,
            PaymentMethod.RAZORPAY, PaymentMethod.STRIPE);

    /**
     * Default assumption (explicitly called out per task instructions): a salon owner with no
     * platform subscription at all (ever) may still operate one salon for free. Any additional
     * salon requires an ACTIVE, paid platform subscription whose plan's {@code maxSalons} (or
     * unlimited) covers the resulting count.
     */
    private static final long FREE_TIER_MAX_SALONS = 1L;

    private final PlatformPlanRepository platformPlanRepository;
    private final PlatformSubscriptionRepository platformSubscriptionRepository;
    private final SalonOwnerRepository salonOwnerRepository;
    private final SalonRepository salonRepository;
    private final PaymentRepository paymentRepository;
    private final TransactionRepository transactionRepository;
    private final PlatformBillingMapper platformBillingMapper;
    private final PaymentGatewayFactory gatewayFactory;
    private final WalletService walletService;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional(readOnly = true)
    public List<com.salofresh.dto.platformbilling.PlatformPlanResponse> listAvailablePlans() {
        return platformPlanRepository.findAllByActiveTrue().stream()
                .map(platformBillingMapper::toPlanResponse)
                .toList();
    }

    @Override
    public SubscriptionPurchaseResult subscribe(Long ownerUserId, SubscribeToPlanRequest request) {
        SalonOwner salonOwner = loadSalonOwner(ownerUserId);
        PlatformPlan plan = loadPlan(request.getPlanId());
        if (!plan.isActive()) {
            throw new BadRequestException("This platform plan is no longer available");
        }

        cancelPriorActiveSubscription(salonOwner.getId());

        Payment payment = Payment.builder()
                .appointment(null)
                .user(salonOwner.getUser())
                .amount(plan.getPrice())
                .currency(AppConstants.CURRENCY_INR)
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus(PaymentStatus.PENDING)
                .build();
        payment = paymentRepository.save(payment);

        PlatformSubscription subscription = createSubscription(payment, plan, salonOwner);

        processInitiation(payment, request.getPaymentMethod(), subscription);

        return buildResult(payment, subscription);
    }

    @Override
    public SubscriptionPurchaseResult confirm(Long ownerUserId, Long paymentId, String gatewayPaymentId, String gatewaySignature) {
        Payment payment = getPaymentOrThrow(paymentId);
        PlatformSubscription subscription = platformSubscriptionRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("PlatformSubscription", "paymentId", paymentId));
        verifyConfirmAccess(payment, ownerUserId);

        if (payment.getPaymentStatus() == PaymentStatus.SUCCESS) {
            return buildResult(payment, subscription);
        }
        if (payment.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new BadRequestException("Payment cannot be confirmed from status " + payment.getPaymentStatus());
        }

        switch (payment.getPaymentMethod()) {
            case CASH -> finalizeSuccess(payment, null);
            case WALLET -> throw new BadRequestException("Wallet payments are finalized automatically and cannot be confirmed");
            case UPI, CREDIT_CARD, DEBIT_CARD, RAZORPAY, STRIPE -> {
                if (payment.getGatewayOrderId() == null) {
                    throw new BadRequestException("Payment has not been initiated with a payment gateway");
                }
                PaymentGateway gateway = gatewayFactory.getGateway(payment.getPaymentMethod());
                boolean verified = gateway.verifyPayment(payment.getGatewayOrderId(), gatewayPaymentId, gatewaySignature);
                if (verified) {
                    finalizeSuccess(payment, gatewayPaymentId);
                } else {
                    finalizeFailure(payment, "Gateway payment verification failed", subscription);
                }
            }
            default -> throw new BadRequestException("Unsupported payment method: " + payment.getPaymentMethod());
        }

        return buildResult(payment, subscription);
    }

    @Override
    @Transactional(readOnly = true)
    public PlatformSubscriptionResponse getMyActiveSubscription(Long ownerUserId) {
        SalonOwner salonOwner = loadSalonOwner(ownerUserId);
        return findUsableActiveSubscription(salonOwner.getId())
                .map(platformBillingMapper::toSubscriptionResponse)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<PlatformSubscriptionResponse> listMySubscriptionHistory(Long ownerUserId, Pageable pageable) {
        SalonOwner salonOwner = loadSalonOwner(ownerUserId);
        Page<PlatformSubscription> page =
                platformSubscriptionRepository.findAllBySalonOwnerIdOrderByCreatedAtDesc(salonOwner.getId(), pageable);
        List<PlatformSubscriptionResponse> content = page.getContent().stream()
                .map(platformBillingMapper::toSubscriptionResponse)
                .toList();
        return PagedResponse.from(page, content);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canCreateAnotherSalon(Long ownerUserId) {
        Optional<SalonOwner> salonOwnerOpt = salonOwnerRepository.findByUserId(ownerUserId);
        if (salonOwnerOpt.isEmpty()) {
            return false;
        }
        SalonOwner salonOwner = salonOwnerOpt.get();
        long currentSalonCount = salonRepository.countByOwnerIdAndDeletedFalse(salonOwner.getId());

        Optional<PlatformSubscription> activeSubscription = findUsableActiveSubscription(salonOwner.getId());
        if (activeSubscription.isEmpty()) {
            return currentSalonCount < FREE_TIER_MAX_SALONS;
        }

        PlatformPlan plan = activeSubscription.get().getPlan();
        return plan.isUnlimitedSalons() || currentSalonCount < plan.getMaxSalons();
    }

    private void cancelPriorActiveSubscription(Long salonOwnerId) {
        platformSubscriptionRepository
                .findFirstBySalonOwnerIdAndStatusOrderByEndDateDesc(salonOwnerId, MembershipStatus.ACTIVE)
                .ifPresent(prior -> {
                    prior.setStatus(MembershipStatus.CANCELLED);
                    platformSubscriptionRepository.save(prior);
                });
    }

    /**
     * The ACTIVE-status subscription (if any) that is also actually paid: either it has no
     * payment attached, or its payment succeeded. Guards against a gateway purchase that is
     * still PENDING confirmation being mistaken for a genuinely active subscription.
     */
    private Optional<PlatformSubscription> findUsableActiveSubscription(Long salonOwnerId) {
        return platformSubscriptionRepository
                .findFirstBySalonOwnerIdAndStatusOrderByEndDateDesc(salonOwnerId, MembershipStatus.ACTIVE)
                .filter(this::isPaid);
    }

    private boolean isPaid(PlatformSubscription subscription) {
        Payment payment = subscription.getPayment();
        return payment == null || payment.getPaymentStatus() == PaymentStatus.SUCCESS;
    }

    private PlatformSubscription createSubscription(Payment payment, PlatformPlan plan, SalonOwner salonOwner) {
        LocalDate today = LocalDate.now();
        LocalDate endDate = plan.getBillingCycle() == BillingCycle.YEARLY ? today.plusDays(365) : today.plusDays(30);
        PlatformSubscription subscription = PlatformSubscription.builder()
                .salonOwner(salonOwner)
                .plan(plan)
                .payment(payment)
                .startDate(today)
                .endDate(endDate)
                .status(MembershipStatus.ACTIVE)
                .build();
        return platformSubscriptionRepository.save(subscription);
    }

    private void processInitiation(Payment payment, PaymentMethod method, PlatformSubscription subscription) {
        if (method == PaymentMethod.CASH) {
            paymentRepository.save(payment);
        } else if (method == PaymentMethod.WALLET) {
            try {
                walletService.debit(payment.getUser(), payment.getAmount(), WalletTransactionSource.BOOKING_PAYMENT,
                        "PLATFORM-SUB-" + payment.getId(), "Platform plan subscription (" + subscription.getPlan().getName() + ")");
                finalizeSuccess(payment, null);
            } catch (BadRequestException e) {
                finalizeFailure(payment, e.getMessage(), subscription);
            }
        } else if (GATEWAY_METHODS.contains(method)) {
            PaymentGateway gateway = gatewayFactory.getGateway(method);
            String gatewayOrderId = gateway.createOrder(payment.getAmount(), payment.getCurrency(), "PSUB-" + payment.getId());
            payment.setGatewayOrderId(gatewayOrderId);
            paymentRepository.save(payment);
        } else {
            throw new BadRequestException("Unsupported payment method: " + method);
        }
    }

    private void finalizeSuccess(Payment payment, String gatewayTransactionId) {
        payment.setPaymentStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(Instant.now());
        payment.setInvoiceNumber(RandomCodeGenerator.generateReferenceNumber(AppConstants.INVOICE_PREFIX));
        payment.setFailureReason(null);
        if (gatewayTransactionId != null) {
            payment.setGatewayTransactionId(gatewayTransactionId);
        }
        payment = paymentRepository.save(payment);

        transactionRepository.save(Transaction.builder()
                .referenceNumber(RandomCodeGenerator.generateReferenceNumber(AppConstants.TRANSACTION_PREFIX))
                .user(payment.getUser())
                .payment(payment)
                .transactionType(WalletTransactionType.DEBIT)
                .amount(payment.getAmount())
                .referenceType("PLATFORM_SUBSCRIPTION_PURCHASE")
                .description("Platform plan subscription payment")
                .build());
    }

    private void finalizeFailure(Payment payment, String reason, PlatformSubscription subscription) {
        payment.setPaymentStatus(PaymentStatus.FAILED);
        payment.setFailureReason(reason);
        paymentRepository.save(payment);

        if (subscription != null) {
            subscription.setStatus(MembershipStatus.CANCELLED);
            subscription.setDeleted(true);
            platformSubscriptionRepository.save(subscription);
        }
    }

    private SubscriptionPurchaseResult buildResult(Payment payment, PlatformSubscription subscription) {
        PlatformSubscriptionResponse subscriptionResponse = payment.getPaymentStatus() == PaymentStatus.SUCCESS
                ? platformBillingMapper.toSubscriptionResponse(subscription)
                : null;

        return SubscriptionPurchaseResult.builder()
                .paymentId(payment.getId())
                .gatewayOrderId(payment.getGatewayOrderId())
                .paymentStatus(payment.getPaymentStatus())
                .subscription(subscriptionResponse)
                .build();
    }

    private Payment getPaymentOrThrow(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));
    }

    private SalonOwner loadSalonOwner(Long userId) {
        return salonOwnerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("SalonOwner", "userId", userId));
    }

    private PlatformPlan loadPlan(Long planId) {
        return platformPlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("PlatformPlan", "id", planId));
    }

    private void verifyConfirmAccess(Payment payment, Long currentUserId) {
        if (payment.getUser().getId().equals(currentUserId) || isAdmin()) {
            return;
        }
        throw new ForbiddenException("You are not allowed to confirm this payment");
    }

    private boolean isAdmin() {
        if (!securityUtils.isAuthenticated()) {
            return false;
        }
        UserPrincipal principal = securityUtils.getCurrentUser();
        return principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_SUPER_ADMIN"));
    }
}
