package com.salofresh.service.impl.membership;

import com.salofresh.common.enums.MembershipStatus;
import com.salofresh.common.enums.PaymentMethod;
import com.salofresh.common.enums.PaymentStatus;
import com.salofresh.common.enums.WalletTransactionSource;
import com.salofresh.common.enums.WalletTransactionType;
import com.salofresh.constant.AppConstants;
import com.salofresh.dto.membership.MembershipSubscriptionResponse;
import com.salofresh.dto.membership.PurchaseMembershipRequest;
import com.salofresh.dto.membership.PurchaseMembershipResponse;
import com.salofresh.dto.payment.ConfirmPaymentRequest;
import com.salofresh.entity.MembershipPlan;
import com.salofresh.entity.MembershipSubscription;
import com.salofresh.entity.Payment;
import com.salofresh.entity.Salon;
import com.salofresh.entity.Transaction;
import com.salofresh.entity.User;
import com.salofresh.exception.BadRequestException;
import com.salofresh.exception.ForbiddenException;
import com.salofresh.exception.ResourceNotFoundException;
import com.salofresh.mapper.membership.MembershipSubscriptionMapper;
import com.salofresh.repository.MembershipPlanRepository;
import com.salofresh.repository.MembershipSubscriptionRepository;
import com.salofresh.repository.PaymentRepository;
import com.salofresh.repository.TransactionRepository;
import com.salofresh.repository.UserRepository;
import com.salofresh.response.PagedResponse;
import com.salofresh.security.SecurityUtils;
import com.salofresh.security.UserPrincipal;
import com.salofresh.service.impl.payment.PaymentGatewayFactory;
import com.salofresh.service.membership.MembershipSubscriptionService;
import com.salofresh.service.payment.PaymentGateway;
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
 * Purchasing/consuming membership subscriptions is deliberately kept separate from
 * {@code PaymentServiceImpl}: a membership {@link Payment} has no linked appointment, and the
 * generic payment service treats any such "orphan" payment as a wallet top-up on success. Reusing
 * it here would silently credit the customer's wallet instead of activating their membership, so
 * this service owns its own initiate/confirm/finalize flow while still delegating to the shared
 * {@link PaymentGateway} abstraction and {@link WalletService}.
 *
 * <p>Because {@code Payment} does not (and per the task scope, must not) carry a reference to the
 * plan being purchased, the resulting {@link MembershipSubscription} is created eagerly, in the
 * same call that creates the Payment, always linked to it via {@code payment_id}. This lets
 * {@link #confirm} locate it later purely from {@code paymentId} (via
 * {@link MembershipSubscriptionRepository#findByPaymentId}) without needing to persist the plan
 * association anywhere else. Until the linked payment actually succeeds, the subscription is not
 * treated as usable: {@link #getActiveSubscriptionForSalon} and {@link #consumeSessionIfApplicable}
 * both gate on {@code payment.paymentStatus == SUCCESS}, and a failed/verification-failed payment
 * cancels its subscription row.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MembershipSubscriptionServiceImpl implements MembershipSubscriptionService {

    private static final Set<PaymentMethod> GATEWAY_METHODS = Set.of(
            PaymentMethod.UPI, PaymentMethod.CREDIT_CARD, PaymentMethod.DEBIT_CARD,
            PaymentMethod.RAZORPAY, PaymentMethod.STRIPE);

    private final MembershipPlanRepository membershipPlanRepository;
    private final MembershipSubscriptionRepository membershipSubscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final MembershipSubscriptionMapper membershipSubscriptionMapper;
    private final PaymentGatewayFactory gatewayFactory;
    private final WalletService walletService;
    private final SecurityUtils securityUtils;

    @Override
    public PurchaseMembershipResponse purchase(Long userId, PurchaseMembershipRequest request) {
        User user = loadUser(userId);
        MembershipPlan plan = loadPlan(request.getPlanId());
        if (!plan.isActive()) {
            throw new BadRequestException("This membership plan is no longer available");
        }

        Payment payment = Payment.builder()
                .appointment(null)
                .user(user)
                .amount(plan.getPrice())
                .currency(AppConstants.CURRENCY_INR)
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus(PaymentStatus.PENDING)
                .build();
        payment = paymentRepository.save(payment);

        MembershipSubscription subscription = createSubscription(payment, plan, user);

        processInitiation(payment, request.getPaymentMethod(), subscription);

        return buildResponse(payment, subscription);
    }

    @Override
    public PurchaseMembershipResponse confirm(Long paymentId, ConfirmPaymentRequest request, Long userId) {
        Payment payment = getPaymentOrThrow(paymentId);
        MembershipSubscription subscription = membershipSubscriptionRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("MembershipSubscription", "paymentId", paymentId));
        verifyConfirmAccess(payment, subscription, userId);

        if (payment.getPaymentStatus() == PaymentStatus.SUCCESS) {
            return buildResponse(payment, subscription);
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
                boolean verified = gateway.verifyPayment(payment.getGatewayOrderId(), request.getGatewayPaymentId(), request.getGatewaySignature());
                if (verified) {
                    finalizeSuccess(payment, request.getGatewayPaymentId());
                } else {
                    finalizeFailure(payment, "Gateway payment verification failed", subscription);
                }
            }
            default -> throw new BadRequestException("Unsupported payment method: " + payment.getPaymentMethod());
        }

        return buildResponse(payment, subscription);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<MembershipSubscriptionResponse> listForUser(Long userId, Pageable pageable) {
        Page<MembershipSubscription> page = membershipSubscriptionRepository.findAllByUserIdOrderByCreatedAtDesc(userId, pageable);
        List<MembershipSubscriptionResponse> content = page.getContent().stream()
                .map(membershipSubscriptionMapper::toResponse)
                .toList();
        return PagedResponse.from(page, content);
    }

    @Override
    @Transactional(readOnly = true)
    public MembershipSubscriptionResponse getActiveSubscriptionForSalon(Long userId, Long salonId) {
        return findUsableSubscription(userId, salonId)
                .map(membershipSubscriptionMapper::toResponse)
                .orElse(null);
    }

    @Override
    public MembershipSubscription consumeSessionIfApplicable(Long userId, Long salonId) {
        MembershipSubscription subscription = findUsableSubscription(userId, salonId).orElse(null);
        if (subscription == null) {
            return null;
        }
        if (!subscription.hasSessionsAvailable()) {
            throw new BadRequestException("This membership subscription has no remaining sessions");
        }
        if (subscription.getRemainingSessions() != null) {
            subscription.setRemainingSessions(subscription.getRemainingSessions() - 1);
            subscription = membershipSubscriptionRepository.save(subscription);
        }
        return subscription;
    }

    private Optional<MembershipSubscription> findUsableSubscription(Long userId, Long salonId) {
        LocalDate today = LocalDate.now();
        return membershipSubscriptionRepository
                .findAllByUserIdAndPlan_Salon_IdAndStatus(userId, salonId, MembershipStatus.ACTIVE)
                .stream()
                .filter(sub -> !sub.getEndDate().isBefore(today))
                .filter(this::isPaid)
                .findFirst();
    }

    private boolean isPaid(MembershipSubscription subscription) {
        Payment payment = subscription.getPayment();
        return payment == null || payment.getPaymentStatus() == PaymentStatus.SUCCESS;
    }

    private MembershipSubscription createSubscription(Payment payment, MembershipPlan plan, User user) {
        LocalDate today = LocalDate.now();
        MembershipSubscription subscription = MembershipSubscription.builder()
                .user(user)
                .plan(plan)
                .payment(payment)
                .startDate(today)
                .endDate(today.plusDays(plan.getValidityDays()))
                .remainingSessions(plan.getTotalSessions())
                .status(MembershipStatus.ACTIVE)
                .build();
        return membershipSubscriptionRepository.save(subscription);
    }

    private void processInitiation(Payment payment, PaymentMethod method, MembershipSubscription subscription) {
        if (method == PaymentMethod.CASH) {
            paymentRepository.save(payment);
        } else if (method == PaymentMethod.WALLET) {
            try {
                walletService.debit(payment.getUser(), payment.getAmount(), WalletTransactionSource.BOOKING_PAYMENT,
                        "MEMBERSHIP-" + payment.getId(), "Membership plan purchase (" + subscription.getPlan().getName() + ")");
                finalizeSuccess(payment, null);
            } catch (BadRequestException e) {
                finalizeFailure(payment, e.getMessage(), subscription);
            }
        } else if (GATEWAY_METHODS.contains(method)) {
            PaymentGateway gateway = gatewayFactory.getGateway(method);
            String gatewayOrderId = gateway.createOrder(payment.getAmount(), payment.getCurrency(), "MEM-" + payment.getId());
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
                .referenceType("MEMBERSHIP_PURCHASE")
                .description("Membership plan purchase payment")
                .build());
    }

    private void finalizeFailure(Payment payment, String reason, MembershipSubscription subscription) {
        payment.setPaymentStatus(PaymentStatus.FAILED);
        payment.setFailureReason(reason);
        paymentRepository.save(payment);

        if (subscription != null) {
            subscription.setStatus(MembershipStatus.CANCELLED);
            subscription.setDeleted(true);
            membershipSubscriptionRepository.save(subscription);
        }
    }

    private PurchaseMembershipResponse buildResponse(Payment payment, MembershipSubscription subscription) {
        MembershipSubscriptionResponse subscriptionResponse = payment.getPaymentStatus() == PaymentStatus.SUCCESS
                ? membershipSubscriptionMapper.toResponse(subscription)
                : null;

        return PurchaseMembershipResponse.builder()
                .paymentId(payment.getId())
                .gatewayOrderId(payment.getGatewayOrderId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .paymentMethod(payment.getPaymentMethod())
                .paymentStatus(payment.getPaymentStatus())
                .subscription(subscriptionResponse)
                .build();
    }

    private Payment getPaymentOrThrow(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));
    }

    private User loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }

    private MembershipPlan loadPlan(Long planId) {
        return membershipPlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("MembershipPlan", "id", planId));
    }

    private void verifyConfirmAccess(Payment payment, MembershipSubscription subscription, Long currentUserId) {
        if (payment.getUser().getId().equals(currentUserId) || isSalonOwnerOf(subscription, currentUserId) || isAdmin()) {
            return;
        }
        throw new ForbiddenException("You are not allowed to confirm this payment");
    }

    private boolean isSalonOwnerOf(MembershipSubscription subscription, Long userId) {
        Salon salon = subscription.getPlan() != null ? subscription.getPlan().getSalon() : null;
        return salon != null
                && salon.getOwner() != null
                && salon.getOwner().getUser() != null
                && salon.getOwner().getUser().getId().equals(userId);
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
