package com.salofresh.service.impl.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salofresh.common.enums.PaymentStatus;
import com.salofresh.common.enums.WebhookProvider;
import com.salofresh.common.enums.WebhookStatus;
import com.salofresh.entity.Payment;
import com.salofresh.entity.WebhookEventLog;
import com.salofresh.exception.ForbiddenException;
import com.salofresh.repository.PaymentRepository;
import com.salofresh.repository.WebhookEventLogRepository;
import com.salofresh.service.webhook.PaymentWebhookService;
import com.salofresh.service.webhook.WebhookSignatureVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Optional;

/**
 * Handles inbound Razorpay/Stripe payment webhook events: verifies the signature, persists an
 * idempotent {@link WebhookEventLog} row keyed by a stable event id, and makes a narrow,
 * best-effort attempt to reconcile the matching {@link Payment}'s status.
 *
 * <p>Reconciliation deliberately does not call {@code PaymentServiceImpl#confirm}, since that
 * flow expects a client-signed confirmation request; instead it applies a minimal, directly
 * saved status transition. Any failure while parsing the payload or reconciling the payment is
 * swallowed and recorded on the event log rather than propagated, so the gateway always receives
 * a 2xx response once the event has been durably logged (a 5xx here would trigger gateway
 * retry-storms).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentWebhookServiceImpl implements PaymentWebhookService {

    private static final int MAX_EVENT_ID_LENGTH = 150;
    private static final int MAX_ERROR_MESSAGE_LENGTH = 1000;

    private final WebhookSignatureVerifier signatureVerifier;
    private final WebhookEventLogRepository webhookEventLogRepository;
    private final PaymentRepository paymentRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void handleRazorpayEvent(String rawBody, String signatureHeader) {
        if (!signatureVerifier.verifyRazorpaySignature(rawBody, signatureHeader)) {
            throw new ForbiddenException("Invalid webhook signature");
        }

        JsonNode root = parseJson(rawBody);
        String eventType = textOrNull(root, "event");
        String eventId = resolveEventId(root, eventType, rawBody);

        if (webhookEventLogRepository.existsByEventId(eventId)) {
            log.info("Ignoring duplicate Razorpay webhook event {}", eventId);
            return;
        }

        WebhookEventLog eventLog = webhookEventLogRepository.save(WebhookEventLog.builder()
                .provider(WebhookProvider.RAZORPAY)
                .eventId(eventId)
                .eventType(eventType)
                .payload(rawBody)
                .status(WebhookStatus.RECEIVED)
                .receivedAt(Instant.now())
                .build());

        try {
            reconcileRazorpayPayment(root, eventType);
            eventLog.setStatus(WebhookStatus.PROCESSED);
            eventLog.setProcessedAt(Instant.now());
        } catch (Exception e) {
            log.warn("Best-effort Razorpay webhook reconciliation failed for event {}: {}", eventId, e.getMessage(), e);
            eventLog.setStatus(WebhookStatus.FAILED);
            eventLog.setErrorMessage(truncate(e.getMessage()));
        }
        webhookEventLogRepository.save(eventLog);
    }

    @Override
    @Transactional
    public void handleStripeEvent(String rawBody, String signatureHeader) {
        if (!signatureVerifier.verifyStripeSignature(rawBody, signatureHeader)) {
            throw new ForbiddenException("Invalid webhook signature");
        }

        JsonNode root = parseJson(rawBody);
        String eventType = textOrNull(root, "type");
        String eventId = resolveEventId(root, eventType, rawBody);

        if (webhookEventLogRepository.existsByEventId(eventId)) {
            log.info("Ignoring duplicate Stripe webhook event {}", eventId);
            return;
        }

        WebhookEventLog eventLog = webhookEventLogRepository.save(WebhookEventLog.builder()
                .provider(WebhookProvider.STRIPE)
                .eventId(eventId)
                .eventType(eventType)
                .payload(rawBody)
                .status(WebhookStatus.RECEIVED)
                .receivedAt(Instant.now())
                .build());

        try {
            reconcileStripePayment(root, eventType);
            eventLog.setStatus(WebhookStatus.PROCESSED);
            eventLog.setProcessedAt(Instant.now());
        } catch (Exception e) {
            log.warn("Best-effort Stripe webhook reconciliation failed for event {}: {}", eventId, e.getMessage(), e);
            eventLog.setStatus(WebhookStatus.FAILED);
            eventLog.setErrorMessage(truncate(e.getMessage()));
        }
        webhookEventLogRepository.save(eventLog);
    }

    /**
     * Looks up the Payment matching the Razorpay order id embedded in the webhook payload
     * (typically at {@code payload.payment.entity.order_id}) and applies a minimal, direct status
     * update for the {@code payment.captured}/{@code payment.failed} events. Only acts when the
     * payment is still {@code PENDING}, to avoid clobbering a status already finalized via the
     * client-driven confirm flow or a prior webhook delivery.
     */
    private void reconcileRazorpayPayment(JsonNode root, String eventType) {
        JsonNode entity = root.path("payload").path("payment").path("entity");
        if (entity.isMissingNode()) {
            return;
        }
        String orderId = textOrNull(entity, "order_id");
        String paymentId = textOrNull(entity, "id");
        if (!StringUtils.hasText(orderId)) {
            return;
        }

        Optional<Payment> paymentOpt = paymentRepository.findByGatewayOrderId(orderId);
        if (paymentOpt.isEmpty()) {
            log.info("No matching payment found for Razorpay order {}", orderId);
            return;
        }

        Payment payment = paymentOpt.get();
        if (payment.getPaymentStatus() != PaymentStatus.PENDING) {
            return;
        }

        if ("payment.captured".equals(eventType)) {
            payment.setPaymentStatus(PaymentStatus.SUCCESS);
            payment.setPaidAt(Instant.now());
            payment.setFailureReason(null);
            if (StringUtils.hasText(paymentId)) {
                payment.setGatewayTransactionId(paymentId);
            }
            paymentRepository.save(payment);
        } else if ("payment.failed".equals(eventType)) {
            payment.setPaymentStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Razorpay webhook reported payment.failed");
            paymentRepository.save(payment);
        }
    }

    /**
     * Looks up the Payment matching the Stripe PaymentIntent id at {@code data.object.id} and
     * applies a minimal, direct status update for the {@code payment_intent.succeeded}/
     * {@code payment_intent.payment_failed} events, mirroring the guarded, PENDING-only update
     * used for Razorpay above.
     */
    private void reconcileStripePayment(JsonNode root, String eventType) {
        JsonNode object = root.path("data").path("object");
        if (object.isMissingNode()) {
            return;
        }
        String gatewayOrderId = textOrNull(object, "id");
        if (!StringUtils.hasText(gatewayOrderId)) {
            return;
        }

        Optional<Payment> paymentOpt = paymentRepository.findByGatewayOrderId(gatewayOrderId);
        if (paymentOpt.isEmpty()) {
            log.info("No matching payment found for Stripe payment intent {}", gatewayOrderId);
            return;
        }

        Payment payment = paymentOpt.get();
        if (payment.getPaymentStatus() != PaymentStatus.PENDING) {
            return;
        }

        if ("payment_intent.succeeded".equals(eventType)) {
            payment.setPaymentStatus(PaymentStatus.SUCCESS);
            payment.setPaidAt(Instant.now());
            payment.setFailureReason(null);
            payment.setGatewayTransactionId(gatewayOrderId);
            paymentRepository.save(payment);
        } else if ("payment_intent.payment_failed".equals(eventType)) {
            payment.setPaymentStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Stripe webhook reported payment_intent.payment_failed");
            paymentRepository.save(payment);
        }
    }

    private JsonNode parseJson(String rawBody) {
        try {
            return objectMapper.readTree(rawBody);
        } catch (Exception e) {
            log.warn("Failed to parse webhook payload as JSON: {}", e.getMessage());
            return objectMapper.getNodeFactory().objectNode();
        }
    }

    /**
     * Stripe payloads always include a top-level {@code id} (the event id) which is used directly.
     * Razorpay payloads typically do not, so a stable fallback idempotency key is derived by
     * hashing the raw body with SHA-256 and prefixing it with the event type.
     */
    private String resolveEventId(JsonNode root, String eventType, String rawBody) {
        String id = textOrNull(root, "id");
        String eventId = StringUtils.hasText(id)
                ? id
                : (StringUtils.hasText(eventType) ? eventType : "event") + "-" + sha256Hex(rawBody);
        return eventId.length() > MAX_EVENT_ID_LENGTH ? eventId.substring(0, MAX_EVENT_ID_LENGTH) : eventId;
    }

    private String textOrNull(JsonNode node, String field) {
        if (node == null) {
            return null;
        }
        JsonNode value = node.get(field);
        return value != null && !value.isNull() ? value.asText(null) : null;
    }

    private String sha256Hex(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((data == null ? "" : data).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private String truncate(String message) {
        if (message == null) {
            return "Unknown error during webhook reconciliation";
        }
        return message.length() > MAX_ERROR_MESSAGE_LENGTH ? message.substring(0, MAX_ERROR_MESSAGE_LENGTH) : message;
    }
}
