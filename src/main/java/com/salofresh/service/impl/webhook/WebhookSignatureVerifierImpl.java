package com.salofresh.service.impl.webhook;

import com.salofresh.config.AppProperties;
import com.salofresh.service.webhook.WebhookSignatureVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

/**
 * HMAC-SHA256-based verification of Razorpay/Stripe webhook signatures, mirroring the
 * technique used for client-side payment verification in {@code RazorpayPaymentGatewayImpl}
 * and {@code StripePaymentGatewayImpl}, but keyed on each gateway's separate webhook secret.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookSignatureVerifierImpl implements WebhookSignatureVerifier {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final AppProperties appProperties;

    @Override
    public boolean verifyRazorpaySignature(String rawBody, String signatureHeader) {
        if (rawBody == null || !StringUtils.hasText(signatureHeader)) {
            return false;
        }
        String secret = appProperties.getPayment().getRazorpay().getWebhookSecret();
        if (!StringUtils.hasText(secret)) {
            return false;
        }
        try {
            String expected = hmacSha256Hex(rawBody, secret);
            return expected.equalsIgnoreCase(signatureHeader.trim());
        } catch (GeneralSecurityException e) {
            log.error("Failed to compute Razorpay webhook signature", e);
            return false;
        }
    }

    @Override
    public boolean verifyStripeSignature(String rawBody, String signatureHeader) {
        if (rawBody == null || !StringUtils.hasText(signatureHeader)) {
            return false;
        }
        String secret = appProperties.getPayment().getStripe().getWebhookSecret();
        if (!StringUtils.hasText(secret)) {
            return false;
        }

        String timestamp = null;
        java.util.List<String> v1Signatures = new java.util.ArrayList<>();
        for (String part : signatureHeader.split(",")) {
            String[] kv = part.trim().split("=", 2);
            if (kv.length != 2) {
                continue;
            }
            if ("t".equals(kv[0])) {
                timestamp = kv[1];
            } else if ("v1".equals(kv[0])) {
                v1Signatures.add(kv[1]);
            }
        }

        if (!StringUtils.hasText(timestamp) || v1Signatures.isEmpty()) {
            return false;
        }

        try {
            String signedPayload = timestamp + "." + rawBody;
            String expected = hmacSha256Hex(signedPayload, secret);
            for (String candidate : v1Signatures) {
                if (expected.equalsIgnoreCase(candidate.trim())) {
                    return true;
                }
            }
            return false;
        } catch (GeneralSecurityException e) {
            log.error("Failed to compute Stripe webhook signature", e);
            return false;
        }
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
