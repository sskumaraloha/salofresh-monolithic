package com.salofresh.service.impl.payment;

import com.salofresh.config.AppProperties;
import com.salofresh.exception.PaymentException;
import com.salofresh.service.payment.PaymentGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

/**
 * Integrates with Stripe's REST API for PaymentIntent creation and status verification.
 *
 * <p>Stripe does not use an HMAC-over-orderId|paymentId scheme like Razorpay for client-side
 * confirmation; instead we treat the PaymentIntent id as the order id and confirm success by
 * retrieving the intent and checking its status. Network calls are best-effort and any failure is
 * wrapped in a {@link PaymentException}, which is expected in sandboxed/offline environments.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StripePaymentGatewayImpl implements PaymentGateway {

    private static final String PAYMENT_INTENTS_URL = "https://api.stripe.com/v1/payment_intents";
    private static final Set<String> SUCCESS_STATUSES = Set.of("succeeded", "requires_capture");

    private final AppProperties appProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String createOrder(BigDecimal amount, String currency, String receiptId) {
        try {
            String secretKey = appProperties.getPayment().getStripe().getSecretKey();

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("amount", String.valueOf(amount.multiply(BigDecimal.valueOf(100)).longValueExact()));
            body.add("currency", currency.toLowerCase());
            body.add("metadata[receipt_id]", receiptId);
            body.add("payment_method_types[]", "card");

            ResponseEntity<Map> response = restTemplate.exchange(
                    PAYMENT_INTENTS_URL, HttpMethod.POST, new HttpEntity<>(body, buildFormHeaders(secretKey)), Map.class);

            Object id = response.getBody() != null ? response.getBody().get("id") : null;
            if (id == null) {
                throw new PaymentException("Stripe did not return a payment intent id");
            }
            return id.toString();
        } catch (PaymentException e) {
            throw e;
        } catch (RestClientException | ArithmeticException e) {
            log.error("Failed to create Stripe payment intent for receipt {}", receiptId, e);
            throw new PaymentException("Unable to initiate Stripe payment: " + e.getMessage());
        }
    }

    @Override
    public boolean verifyPayment(String orderId, String gatewayPaymentId, String signature) {
        if (orderId == null) {
            return false;
        }
        try {
            String secretKey = appProperties.getPayment().getStripe().getSecretKey();
            HttpHeaders headers = buildFormHeaders(secretKey);
            ResponseEntity<Map> response = restTemplate.exchange(
                    PAYMENT_INTENTS_URL + "/" + orderId, HttpMethod.GET, new HttpEntity<>(headers), Map.class);

            Object status = response.getBody() != null ? response.getBody().get("status") : null;
            return status != null && SUCCESS_STATUSES.contains(status.toString());
        } catch (RestClientException e) {
            log.error("Failed to verify Stripe payment intent {}", orderId, e);
            throw new PaymentException("Unable to verify Stripe payment: " + e.getMessage());
        }
    }

    @Override
    public void refund(String gatewayTransactionId, BigDecimal amount) {
        if (gatewayTransactionId == null) {
            return;
        }
        try {
            String secretKey = appProperties.getPayment().getStripe().getSecretKey();
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("payment_intent", gatewayTransactionId);
            body.add("amount", String.valueOf(amount.multiply(BigDecimal.valueOf(100)).longValueExact()));

            restTemplate.exchange("https://api.stripe.com/v1/refunds", HttpMethod.POST,
                    new HttpEntity<>(body, buildFormHeaders(secretKey)), Map.class);
        } catch (Exception e) {
            log.warn("Best-effort Stripe refund call failed for payment intent {}: {}", gatewayTransactionId, e.getMessage());
        }
    }

    private HttpHeaders buildFormHeaders(String secretKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBearerAuth(secretKey);
        return headers;
    }
}
