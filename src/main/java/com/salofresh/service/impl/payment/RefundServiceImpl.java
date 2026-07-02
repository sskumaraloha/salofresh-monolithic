package com.salofresh.service.impl.payment;

import com.salofresh.common.enums.PaymentMethod;
import com.salofresh.common.enums.PaymentStatus;
import com.salofresh.common.enums.WalletTransactionSource;
import com.salofresh.common.enums.WalletTransactionType;
import com.salofresh.constant.AppConstants;
import com.salofresh.dto.payment.RefundRequest;
import com.salofresh.dto.payment.RefundResponse;
import com.salofresh.entity.Appointment;
import com.salofresh.entity.Payment;
import com.salofresh.entity.Refund;
import com.salofresh.entity.Transaction;
import com.salofresh.event.RefundProcessedEvent;
import com.salofresh.exception.BadRequestException;
import com.salofresh.exception.ForbiddenException;
import com.salofresh.exception.ResourceNotFoundException;
import com.salofresh.mapper.payment.RefundMapper;
import com.salofresh.repository.PaymentRepository;
import com.salofresh.repository.RefundRepository;
import com.salofresh.repository.TransactionRepository;
import com.salofresh.security.SecurityUtils;
import com.salofresh.security.UserPrincipal;
import com.salofresh.service.payment.PaymentGateway;
import com.salofresh.service.payment.RefundService;
import com.salofresh.util.RandomCodeGenerator;
import com.salofresh.wallet.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {

    private static final Set<PaymentMethod> GATEWAY_METHODS = Set.of(
            PaymentMethod.UPI, PaymentMethod.CREDIT_CARD, PaymentMethod.DEBIT_CARD,
            PaymentMethod.RAZORPAY, PaymentMethod.STRIPE);

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final TransactionRepository transactionRepository;
    private final RefundMapper refundMapper;
    private final PaymentGatewayFactory gatewayFactory;
    private final WalletService walletService;
    private final SecurityUtils securityUtils;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public RefundResponse initiateRefund(Long paymentId, RefundRequest request, Long currentUserId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));
        checkOwnerOrAdmin(payment, currentUserId);

        if (payment.getPaymentStatus() != PaymentStatus.SUCCESS && payment.getPaymentStatus() != PaymentStatus.PARTIALLY_REFUNDED) {
            throw new BadRequestException("Only successful payments can be refunded (current status: " + payment.getPaymentStatus() + ")");
        }

        BigDecimal remaining = payment.getAmount().subtract(payment.getRefundedAmount());
        BigDecimal refundAmount = request.getAmount() != null ? request.getAmount() : remaining;

        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Refund amount must be greater than zero");
        }
        if (refundAmount.compareTo(remaining) > 0) {
            throw new BadRequestException("Refund amount exceeds the remaining refundable amount of " + remaining);
        }

        Refund refund = refundRepository.save(Refund.builder()
                .payment(payment)
                .amount(refundAmount)
                .reason(request.getReason())
                .status(PaymentStatus.PENDING)
                .build());

        if (GATEWAY_METHODS.contains(payment.getPaymentMethod())) {
            try {
                PaymentGateway gateway = gatewayFactory.getGateway(payment.getPaymentMethod());
                gateway.refund(payment.getGatewayTransactionId(), refundAmount);
            } catch (Exception e) {
                log.warn("Gateway refund call failed for payment {}, proceeding with internal bookkeeping anyway: {}",
                        paymentId, e.getMessage());
            }
        }

        refund.setStatus(PaymentStatus.SUCCESS);
        refund.setProcessedAt(Instant.now());
        refund = refundRepository.save(refund);

        payment.setRefundedAmount(payment.getRefundedAmount().add(refundAmount));
        payment.setPaymentStatus(payment.getRefundedAmount().compareTo(payment.getAmount()) >= 0
                ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED);
        paymentRepository.save(payment);

        transactionRepository.save(Transaction.builder()
                .referenceNumber(RandomCodeGenerator.generateReferenceNumber(AppConstants.TRANSACTION_PREFIX))
                .user(payment.getUser())
                .payment(payment)
                .transactionType(WalletTransactionType.CREDIT)
                .amount(refundAmount)
                .referenceType("REFUND")
                .description("Refund for payment #" + payment.getId() + ": " + request.getReason())
                .build());

        if (payment.getPaymentMethod() == PaymentMethod.WALLET) {
            walletService.credit(payment.getUser(), refundAmount, WalletTransactionSource.REFUND,
                    "REFUND-" + refund.getId(), "Refund for payment #" + payment.getId());
        }

        eventPublisher.publishEvent(new RefundProcessedEvent(refund));

        return refundMapper.toResponse(refund);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RefundResponse> listForPayment(Long paymentId, Long currentUserId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));
        checkOwnerOrAdmin(payment, currentUserId);
        return refundRepository.findAllByPaymentId(paymentId).stream().map(refundMapper::toResponse).toList();
    }

    private void checkOwnerOrAdmin(Payment payment, Long currentUserId) {
        if (isSalonOwnerOf(payment, currentUserId) || isAdmin()) {
            return;
        }
        throw new ForbiddenException("Only the salon owner or an administrator can refund this payment");
    }

    private boolean isSalonOwnerOf(Payment payment, Long userId) {
        Appointment appointment = payment.getAppointment();
        return appointment != null
                && appointment.getSalon() != null
                && appointment.getSalon().getOwner() != null
                && appointment.getSalon().getOwner().getUser() != null
                && appointment.getSalon().getOwner().getUser().getId().equals(userId);
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
