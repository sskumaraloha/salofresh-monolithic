package com.salofresh.service.impl.giftcard;

import com.salofresh.common.enums.GiftCardStatus;
import com.salofresh.common.enums.PaymentMethod;
import com.salofresh.common.enums.PaymentStatus;
import com.salofresh.common.enums.WalletTransactionSource;
import com.salofresh.constant.AppConstants;
import com.salofresh.dto.giftcard.GiftCardResponse;
import com.salofresh.dto.giftcard.PurchaseGiftCardRequest;
import com.salofresh.dto.giftcard.RedeemGiftCardResponse;
import com.salofresh.email.EmailService;
import com.salofresh.email.EmailTemplateBuilder;
import com.salofresh.entity.GiftCard;
import com.salofresh.entity.Payment;
import com.salofresh.entity.User;
import com.salofresh.event.PaymentFailedEvent;
import com.salofresh.event.PaymentSuccessEvent;
import com.salofresh.exception.BadRequestException;
import com.salofresh.exception.ForbiddenException;
import com.salofresh.exception.ResourceNotFoundException;
import com.salofresh.mapper.giftcard.GiftCardMapper;
import com.salofresh.repository.GiftCardRepository;
import com.salofresh.repository.PaymentRepository;
import com.salofresh.repository.UserRepository;
import com.salofresh.response.PagedResponse;
import com.salofresh.service.giftcard.GiftCardService;
import com.salofresh.service.impl.payment.PaymentGatewayFactory;
import com.salofresh.service.payment.PaymentGateway;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class GiftCardServiceImpl implements GiftCardService {

    private static final Set<PaymentMethod> GATEWAY_METHODS = Set.of(
            PaymentMethod.UPI, PaymentMethod.CREDIT_CARD, PaymentMethod.DEBIT_CARD,
            PaymentMethod.RAZORPAY, PaymentMethod.STRIPE);

    private static final int DEFAULT_VALIDITY_DAYS = 365;
    private static final String CODE_PREFIX = "GC";
    private static final int CODE_RANDOM_LENGTH = 12;
    private static final int MAX_CODE_ATTEMPTS = 10;

    private final GiftCardRepository giftCardRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final GiftCardMapper giftCardMapper;
    private final PaymentGatewayFactory gatewayFactory;
    private final WalletService walletService;
    private final EmailService emailService;
    private final EmailTemplateBuilder emailTemplateBuilder;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * In-memory holding area for the recipient/message/validity details of a gateway-method
     * purchase between {@link #purchase} (which only creates the Payment + gateway order) and
     * {@link #confirm} (which actually issues the gift card). The GiftCard entity/migration
     * intentionally carries no "pending purchase" state, so this cannot be persisted without
     * altering the schema; entries are removed as soon as the payment reaches a final state.
     */
    private final Map<Long, PendingGiftCardPurchase> pendingPurchases = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public GiftCardResponse purchase(Long userId, PurchaseGiftCardRequest request) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Gift card amount must be greater than zero");
        }
        if (request.getPaymentMethod() == PaymentMethod.CASH) {
            throw new BadRequestException("Gift cards must be prepaid; cash is not a supported payment method");
        }

        User purchaser = loadUser(userId);
        int validityDays = request.getValidityDays() != null ? request.getValidityDays() : DEFAULT_VALIDITY_DAYS;
        String code = generateUniqueCode();

        Payment payment = Payment.builder()
                .appointment(null)
                .user(purchaser)
                .amount(request.getAmount())
                .currency(AppConstants.CURRENCY_INR)
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus(PaymentStatus.PENDING)
                .build();
        payment = paymentRepository.save(payment);

        if (request.getPaymentMethod() == PaymentMethod.WALLET) {
            return purchaseViaWallet(payment, purchaser, code, request, validityDays);
        }
        if (GATEWAY_METHODS.contains(request.getPaymentMethod())) {
            return purchaseViaGateway(payment, request, code, validityDays);
        }
        throw new BadRequestException("Unsupported payment method: " + request.getPaymentMethod());
    }

    private GiftCardResponse purchaseViaWallet(Payment payment, User purchaser, String code,
                                                PurchaseGiftCardRequest request, int validityDays) {
        try {
            walletService.debit(purchaser, payment.getAmount(), WalletTransactionSource.BOOKING_PAYMENT,
                    "GIFTCARD-PAYMENT-" + payment.getId(), "Gift card purchase");
            finalizeSuccess(payment, null);
        } catch (BadRequestException e) {
            finalizeFailure(payment, e.getMessage());
            throw e;
        }

        GiftCard giftCard = issueGiftCard(purchaser, payment.getAmount(), code, request.getRecipientName(),
                request.getRecipientEmail(), request.getRecipientPhone(), request.getMessage(), validityDays);
        return toResponseWithPayment(giftCard, payment);
    }

    private GiftCardResponse purchaseViaGateway(Payment payment, PurchaseGiftCardRequest request, String code,
                                                 int validityDays) {
        PaymentGateway gateway = gatewayFactory.getGateway(payment.getPaymentMethod());
        String gatewayOrderId = gateway.createOrder(payment.getAmount(), payment.getCurrency(), "GIFTCARD-" + payment.getId());
        payment.setGatewayOrderId(gatewayOrderId);
        payment = paymentRepository.save(payment);

        pendingPurchases.put(payment.getId(), new PendingGiftCardPurchase(code, request.getRecipientName(),
                request.getRecipientEmail(), request.getRecipientPhone(), request.getMessage(), validityDays));

        return GiftCardResponse.builder()
                .initialAmount(payment.getAmount())
                .balance(payment.getAmount())
                .recipientName(request.getRecipientName())
                .paymentId(payment.getId())
                .paymentStatus(payment.getPaymentStatus())
                .gatewayOrderId(payment.getGatewayOrderId())
                .build();
    }

    @Override
    @Transactional
    public GiftCardResponse confirm(Long paymentId, String gatewayPaymentId, String gatewaySignature, Long userId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));
        if (!payment.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You are not allowed to confirm this payment");
        }
        if (payment.getPaymentStatus() == PaymentStatus.SUCCESS) {
            throw new BadRequestException("This payment has already been confirmed");
        }
        if (payment.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new BadRequestException("Payment cannot be confirmed from status " + payment.getPaymentStatus());
        }
        if (!GATEWAY_METHODS.contains(payment.getPaymentMethod())) {
            throw new BadRequestException("This payment method does not require gateway confirmation");
        }
        if (payment.getGatewayOrderId() == null) {
            throw new BadRequestException("Payment has not been initiated with a payment gateway");
        }
        PendingGiftCardPurchase pending = pendingPurchases.get(paymentId);
        if (pending == null) {
            throw new BadRequestException("No pending gift card purchase found for this payment");
        }

        PaymentGateway gateway = gatewayFactory.getGateway(payment.getPaymentMethod());
        boolean verified = gateway.verifyPayment(payment.getGatewayOrderId(), gatewayPaymentId, gatewaySignature);
        if (!verified) {
            finalizeFailure(payment, "Gateway payment verification failed");
            pendingPurchases.remove(paymentId);
            throw new BadRequestException("Gateway payment verification failed");
        }

        finalizeSuccess(payment, gatewayPaymentId);
        GiftCard giftCard = issueGiftCard(payment.getUser(), payment.getAmount(), pending.code(), pending.recipientName(),
                pending.recipientEmail(), pending.recipientPhone(), pending.message(), pending.validityDays());
        pendingPurchases.remove(paymentId);

        return toResponseWithPayment(giftCard, payment);
    }

    @Override
    @Transactional
    public RedeemGiftCardResponse redeem(Long userId, String code) {
        User user = loadUser(userId);
        GiftCard giftCard = giftCardRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new ResourceNotFoundException("GiftCard", "code", code));

        if (giftCard.getStatus() != GiftCardStatus.ACTIVE) {
            throw new BadRequestException("This gift card is not active (status: " + giftCard.getStatus() + ")");
        }
        if (giftCard.getExpiryDate().isBefore(LocalDate.now())) {
            giftCard.setStatus(GiftCardStatus.EXPIRED);
            giftCardRepository.save(giftCard);
            throw new BadRequestException("This gift card has expired");
        }

        BigDecimal amount = giftCard.getBalance();
        walletService.credit(user, amount, WalletTransactionSource.CASHBACK,
                "GIFTCARD-" + giftCard.getCode(), "Gift card redemption: " + giftCard.getCode());

        giftCard.setStatus(GiftCardStatus.REDEEMED);
        giftCard.setRedeemedBy(user);
        giftCard.setRedeemedAt(Instant.now());
        giftCard.setBalance(BigDecimal.ZERO);
        giftCardRepository.save(giftCard);

        return RedeemGiftCardResponse.builder()
                .creditedAmount(amount)
                .newWalletBalance(walletService.getBalance(userId))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<GiftCardResponse> listPurchasedByMe(Long userId, Pageable pageable) {
        Page<GiftCard> page = giftCardRepository.findAllByPurchasedByIdOrderByCreatedAtDesc(userId, pageable);
        List<GiftCardResponse> content = page.getContent().stream().map(giftCardMapper::toResponse).toList();
        return PagedResponse.from(page, content);
    }

    @Override
    @Transactional(readOnly = true)
    public GiftCardResponse checkBalance(String code) {
        GiftCard giftCard = giftCardRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new ResourceNotFoundException("GiftCard", "code", code));
        return giftCardMapper.toResponse(giftCard);
    }

    private GiftCard issueGiftCard(User purchaser, BigDecimal amount, String code, String recipientName,
                                    String recipientEmail, String recipientPhone, String message, int validityDays) {
        Instant issuedAt = Instant.now();
        GiftCard giftCard = GiftCard.builder()
                .code(code)
                .initialAmount(amount)
                .balance(amount)
                .purchasedBy(purchaser)
                .recipientName(recipientName)
                .recipientEmail(recipientEmail)
                .recipientPhone(recipientPhone)
                .message(message)
                .issuedAt(issuedAt)
                .expiryDate(LocalDate.now().plusDays(validityDays))
                .status(GiftCardStatus.ACTIVE)
                .build();
        giftCard = giftCardRepository.save(giftCard);

        if (recipientEmail != null && !recipientEmail.isBlank()) {
            emailService.sendHtmlEmail(recipientEmail, "You've received a SaloFresh Gift Card!",
                    emailTemplateBuilder.buildGiftCardEmail(recipientName, code, amount, message, giftCard.getExpiryDate()));
        }
        return giftCard;
    }

    private String generateUniqueCode() {
        String code;
        int attempts = 0;
        do {
            if (attempts++ >= MAX_CODE_ATTEMPTS) {
                throw new IllegalStateException("Unable to generate a unique gift card code");
            }
            code = CODE_PREFIX + RandomCodeGenerator.generateAlphanumeric(CODE_RANDOM_LENGTH);
        } while (giftCardRepository.existsByCodeIgnoreCase(code));
        return code;
    }

    private void finalizeSuccess(Payment payment, String gatewayTransactionId) {
        payment.setPaymentStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(Instant.now());
        payment.setInvoiceNumber(RandomCodeGenerator.generateReferenceNumber(AppConstants.INVOICE_PREFIX));
        payment.setFailureReason(null);
        if (gatewayTransactionId != null) {
            payment.setGatewayTransactionId(gatewayTransactionId);
        }
        paymentRepository.save(payment);
        eventPublisher.publishEvent(new PaymentSuccessEvent(payment));
    }

    private void finalizeFailure(Payment payment, String reason) {
        payment.setPaymentStatus(PaymentStatus.FAILED);
        payment.setFailureReason(reason);
        paymentRepository.save(payment);
        eventPublisher.publishEvent(new PaymentFailedEvent(payment));
    }

    private GiftCardResponse toResponseWithPayment(GiftCard giftCard, Payment payment) {
        GiftCardResponse response = giftCardMapper.toResponse(giftCard);
        response.setPaymentId(payment.getId());
        response.setPaymentStatus(payment.getPaymentStatus());
        return response;
    }

    private User loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }

    private record PendingGiftCardPurchase(String code, String recipientName, String recipientEmail,
                                            String recipientPhone, String message, int validityDays) {
    }
}
