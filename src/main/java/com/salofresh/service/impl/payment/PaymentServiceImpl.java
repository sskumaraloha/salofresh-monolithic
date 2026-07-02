package com.salofresh.service.impl.payment;

import com.salofresh.common.enums.BookingStatus;
import com.salofresh.common.enums.PaymentMethod;
import com.salofresh.common.enums.PaymentStatus;
import com.salofresh.common.enums.WalletTransactionSource;
import com.salofresh.common.enums.WalletTransactionType;
import com.salofresh.constant.AppConstants;
import com.salofresh.dto.payment.ConfirmPaymentRequest;
import com.salofresh.dto.payment.InitiatePaymentRequest;
import com.salofresh.dto.payment.InitiatePaymentResponse;
import com.salofresh.dto.payment.InvoiceResponse;
import com.salofresh.dto.payment.PaymentResponse;
import com.salofresh.dto.wallet.WalletTopupRequest;
import com.salofresh.entity.Appointment;
import com.salofresh.entity.AppointmentService;
import com.salofresh.entity.Payment;
import com.salofresh.entity.Transaction;
import com.salofresh.entity.User;
import com.salofresh.event.PaymentFailedEvent;
import com.salofresh.event.PaymentSuccessEvent;
import com.salofresh.exception.BadRequestException;
import com.salofresh.exception.ForbiddenException;
import com.salofresh.exception.ResourceNotFoundException;
import com.salofresh.mapper.payment.PaymentMapper;
import com.salofresh.repository.AppointmentRepository;
import com.salofresh.repository.AppointmentServiceRepository;
import com.salofresh.repository.PaymentRepository;
import com.salofresh.repository.TransactionRepository;
import com.salofresh.repository.UserRepository;
import com.salofresh.response.PagedResponse;
import com.salofresh.security.SecurityUtils;
import com.salofresh.security.UserPrincipal;
import com.salofresh.service.payment.PaymentGateway;
import com.salofresh.service.payment.PaymentService;
import com.salofresh.util.RandomCodeGenerator;
import com.salofresh.wallet.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final Set<PaymentMethod> GATEWAY_METHODS = Set.of(
            PaymentMethod.UPI, PaymentMethod.CREDIT_CARD, PaymentMethod.DEBIT_CARD,
            PaymentMethod.RAZORPAY, PaymentMethod.STRIPE);

    private final PaymentRepository paymentRepository;
    private final TransactionRepository transactionRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentServiceRepository appointmentServiceRepository;
    private final UserRepository userRepository;
    private final PaymentMapper paymentMapper;
    private final PaymentGatewayFactory gatewayFactory;
    private final WalletService walletService;
    private final SecurityUtils securityUtils;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public InitiatePaymentResponse initiate(Long appointmentId, InitiatePaymentRequest request, Long currentUserId) {
        Payment payment = paymentRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "appointmentId", appointmentId));

        if (!payment.getUser().getId().equals(currentUserId)) {
            throw new ForbiddenException("You are not allowed to pay for this appointment");
        }
        if (payment.getPaymentStatus() != PaymentStatus.PENDING && payment.getPaymentStatus() != PaymentStatus.FAILED) {
            throw new BadRequestException("This payment has already been processed (status: " + payment.getPaymentStatus() + ")");
        }

        return processInitiation(payment, request.getPaymentMethod());
    }

    @Override
    @Transactional
    public InitiatePaymentResponse initiateWalletTopup(WalletTopupRequest request, Long currentUserId) {
        if (request.getPaymentMethod() == PaymentMethod.WALLET) {
            throw new BadRequestException("Wallet balance cannot be used to top up the wallet itself");
        }
        User user = loadUser(currentUserId);

        Payment payment = Payment.builder()
                .appointment(null)
                .user(user)
                .amount(request.getAmount())
                .currency(AppConstants.CURRENCY_INR)
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus(PaymentStatus.PENDING)
                .build();
        payment = paymentRepository.save(payment);

        return processInitiation(payment, request.getPaymentMethod());
    }

    private InitiatePaymentResponse processInitiation(Payment payment, PaymentMethod method) {
        payment.setPaymentMethod(method);

        if (method == PaymentMethod.CASH) {
            paymentRepository.save(payment);
        } else if (method == PaymentMethod.WALLET) {
            try {
                walletService.debit(payment.getUser(), payment.getAmount(), WalletTransactionSource.BOOKING_PAYMENT,
                        "PAYMENT-" + payment.getId(), "Payment for " + describePayment(payment));
                finalizeSuccess(payment, null);
            } catch (BadRequestException e) {
                finalizeFailure(payment, e.getMessage());
            }
        } else if (GATEWAY_METHODS.contains(method)) {
            PaymentGateway gateway = gatewayFactory.getGateway(method);
            String gatewayOrderId = gateway.createOrder(payment.getAmount(), payment.getCurrency(), "PAY-" + payment.getId());
            payment.setGatewayOrderId(gatewayOrderId);
            paymentRepository.save(payment);
        } else {
            throw new BadRequestException("Unsupported payment method: " + method);
        }

        return InitiatePaymentResponse.builder()
                .paymentId(payment.getId())
                .gatewayOrderId(payment.getGatewayOrderId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .paymentMethod(payment.getPaymentMethod())
                .paymentStatus(payment.getPaymentStatus())
                .build();
    }

    @Override
    @Transactional
    public PaymentResponse confirm(Long paymentId, ConfirmPaymentRequest request, Long currentUserId) {
        Payment payment = getPaymentOrThrow(paymentId);
        checkPayerOrSalonOwnerOrAdmin(payment, currentUserId);

        if (payment.getPaymentStatus() == PaymentStatus.SUCCESS) {
            return paymentMapper.toResponse(payment);
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
                    finalizeFailure(payment, "Gateway payment verification failed");
                }
            }
            default -> throw new BadRequestException("Unsupported payment method: " + payment.getPaymentMethod());
        }

        return paymentMapper.toResponse(payment);
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

        Appointment appointment = payment.getAppointment();
        String referenceType = appointment != null ? "APPOINTMENT_PAYMENT" : "WALLET_TOPUP";

        transactionRepository.save(Transaction.builder()
                .referenceNumber(RandomCodeGenerator.generateReferenceNumber(AppConstants.TRANSACTION_PREFIX))
                .user(payment.getUser())
                .payment(payment)
                .transactionType(WalletTransactionType.DEBIT)
                .amount(payment.getAmount())
                .referenceType(referenceType)
                .description("Payment for " + describePayment(payment))
                .build());

        if (appointment != null) {
            appointment.setStatus(BookingStatus.CONFIRMED);
            appointmentRepository.save(appointment);
        } else {
            // Standalone payment with no linked appointment is treated as a wallet top-up.
            walletService.credit(payment.getUser(), payment.getAmount(), WalletTransactionSource.TOPUP,
                    "PAYMENT-" + payment.getId(), "Wallet top-up via " + payment.getPaymentMethod());
        }

        eventPublisher.publishEvent(new PaymentSuccessEvent(payment));
    }

    private void finalizeFailure(Payment payment, String reason) {
        payment.setPaymentStatus(PaymentStatus.FAILED);
        payment.setFailureReason(reason);
        paymentRepository.save(payment);
        eventPublisher.publishEvent(new PaymentFailedEvent(payment));
    }

    private String describePayment(Payment payment) {
        return payment.getAppointment() != null
                ? "appointment " + payment.getAppointment().getBookingNumber()
                : "wallet top-up";
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getById(Long paymentId, Long currentUserId) {
        Payment payment = getPaymentOrThrow(paymentId);
        checkPayerOrSalonOwnerOrAdmin(payment, currentUserId);
        return paymentMapper.toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<PaymentResponse> listForUser(Long currentUserId, Pageable pageable) {
        Page<Payment> page = paymentRepository.findAllByUserId(currentUserId, pageable);
        List<PaymentResponse> content = page.getContent().stream().map(paymentMapper::toResponse).toList();
        return PagedResponse.from(page, content);
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoice(Long paymentId, Long currentUserId) {
        Payment payment = getPaymentOrThrow(paymentId);
        checkPayerOrSalonOwnerOrAdmin(payment, currentUserId);

        if (payment.getPaymentStatus() != PaymentStatus.SUCCESS
                && payment.getPaymentStatus() != PaymentStatus.PARTIALLY_REFUNDED
                && payment.getPaymentStatus() != PaymentStatus.REFUNDED) {
            throw new BadRequestException("Invoice is only available once payment has succeeded");
        }

        Appointment appointment = payment.getAppointment();
        List<InvoiceResponse.LineItem> lineItems = List.of();
        String bookingNumber = null;
        String salonName = null;
        BigDecimal subtotal = payment.getAmount();
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal taxAmount = BigDecimal.ZERO;

        if (appointment != null) {
            bookingNumber = appointment.getBookingNumber();
            salonName = appointment.getSalon() != null ? appointment.getSalon().getName() : null;
            discountAmount = appointment.getDiscountAmount();
            taxAmount = appointment.getTaxAmount();
            subtotal = appointment.getTotalAmount();

            List<AppointmentService> services = appointmentServiceRepository.findAllByAppointmentId(appointment.getId());
            lineItems = services.stream()
                    .map(s -> InvoiceResponse.LineItem.builder()
                            .serviceName(s.getServiceName())
                            .quantity(s.getQuantity())
                            .unitPrice(s.getPrice())
                            .lineTotal(s.getPrice().multiply(BigDecimal.valueOf(s.getQuantity())))
                            .build())
                    .toList();
        }

        return InvoiceResponse.builder()
                .invoiceNumber(payment.getInvoiceNumber())
                .bookingNumber(bookingNumber)
                .customerName(payment.getUser().getFullName())
                .salonName(salonName)
                .lineItems(lineItems)
                .subtotal(subtotal)
                .discountAmount(discountAmount)
                .taxAmount(taxAmount)
                .totalAmount(payment.getAmount())
                .currency(payment.getCurrency())
                .paymentMethod(payment.getPaymentMethod())
                .paidAt(payment.getPaidAt())
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

    private void checkPayerOrSalonOwnerOrAdmin(Payment payment, Long currentUserId) {
        if (payment.getUser().getId().equals(currentUserId) || isSalonOwnerOf(payment, currentUserId) || isAdmin()) {
            return;
        }
        throw new ForbiddenException("You are not allowed to access this payment");
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
