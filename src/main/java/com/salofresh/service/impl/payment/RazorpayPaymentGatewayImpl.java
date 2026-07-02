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
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;

/**
 * Integrates with Razorpay's REST API for order creation and payment verification. Used for the
 * RAZORPAY payment method as well as UPI/CREDIT_CARD/DEBIT_CARD, which are routed through Razorpay
 * checkout in this platform.
 *
 * <p>Network calls are best-effort: any transport/API failure is wrapped in a {@link PaymentException}
 * so callers can treat it as a normal payment failure rather than a server error. This is expected to
 * fail in sandboxed/offline environments where outbound calls to api.razorpay.com are blocked.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RazorpayPaymentGatewayImpl implements PaymentGateway {

    private static final String ORDERS_URL = "https://api.razorpay.com/v1/orders";
    private static final String REFUND_URL_TEMPLATE = "https://api.razorpay.com/v1/payments/%s/refund";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final AppProperties appProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String createOrder(BigDecimal amount, String currency, String receiptId) {
        try {
            AppProperties.Payment.Razorpay config = appProperties.getPayment().getRazorpay();

            Map<String, Object> body = new HashMap<>();
            body.put("amount", amount.multiply(BigDecimal.valueOf(100)).longValueExact());
            body.put("currency", currency);
            body.put("receipt", receiptId);

            ResponseEntity<Map> response = restTemplate.exchange(
                    ORDERS_URL, HttpMethod.POST, new HttpEntity<>(body, buildJsonHeaders(config.getKeyId(), config.getKeySecret())), Map.class);

            Object orderId = response.getBody() != null ? response.getBody().get("id") : null;
            if (orderId == null) {
                throw new PaymentException("Razorpay did not return an order id");
            }
            return orderId.toString();
        } catch (PaymentException e) {
            throw e;
        } catch (RestClientException | ArithmeticException e) {
            log.error("Failed to create Razorpay order for receipt {}", receiptId, e);
            throw new PaymentException("Unable to initiate Razorpay payment: " + e.getMessage());
        }
    }

    @Override
    public boolean verifyPayment(String orderId, String gatewayPaymentId, String signature) {
        if (orderId == null || gatewayPaymentId == null || signature == null) {
            return false;
        }
        try {
            String secret = appProperties.getPayment().getRazorpay().getKeySecret();
            String payload = orderId + "|" + gatewayPaymentId;
            String expectedSignature = hmacSha256Hex(payload, secret);
            return expectedSignature.equalsIgnoreCase(signature);
        } catch (GeneralSecurityException e) {
            log.error("Failed to compute Razorpay signature for order {}", orderId, e);
            throw new PaymentException("Unable to verify Razorpay payment signature");
        }
    }

    @Override
    public void refund(String gatewayTransactionId, BigDecimal amount) {
        if (gatewayTransactionId == null) {
            return;
        }
        try {
            AppProperties.Payment.Razorpay config = appProperties.getPayment().getRazorpay();
            Map<String, Object> body = new HashMap<>();
            body.put("amount", amount.multiply(BigDecimal.valueOf(100)).longValueExact());

            String url = REFUND_URL_TEMPLATE.formatted(gatewayTransactionId);
            restTemplate.exchange(url, HttpMethod.POST,
                    new HttpEntity<>(body, buildJsonHeaders(config.getKeyId(), config.getKeySecret())), Map.class);
        } catch (Exception e) {
            log.warn("Best-effort Razorpay refund call failed for payment {}: {}", gatewayTransactionId, e.getMessage());
        }
    }

    private HttpHeaders buildJsonHeaders(String keyId, String keySecret) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(keyId, keySecret);
        return headers;
    }

    private static String hmacSha256Hex(String data, String key) throws GeneralSecurityException {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
