package com.salofresh.controller.webhook;

import com.salofresh.constant.AppConstants;
import com.salofresh.service.webhook.PaymentWebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Publicly-reachable endpoints that receive Razorpay/Stripe payment webhook events. There is no
 * JWT here since payment gateways call these directly and cannot present platform credentials;
 * the HMAC signature verification performed inside {@link PaymentWebhookService} is the actual
 * security control. These paths are additionally listed in
 * {@code SecurityConstants.PUBLIC_ENDPOINTS} so Spring Security lets unauthenticated requests
 * reach them at all.
 *
 * <p>Both endpoints always answer 200 once the signature check passes, even if downstream
 * reconciliation had issues, so gateways don't retry-storm a delivery that was already durably
 * logged. Only an invalid signature yields a non-2xx (403, via {@code ForbiddenException}).
 */
@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/webhooks")
@RequiredArgsConstructor
@PreAuthorize("permitAll()")
@Tag(name = "Payment Webhooks", description = "Inbound Razorpay/Stripe payment gateway webhook callbacks")
public class PaymentWebhookController {

    private final PaymentWebhookService paymentWebhookService;

    @PostMapping("/razorpay")
    @Operation(summary = "Receive a Razorpay payment webhook event",
            description = "Verifies the X-Razorpay-Signature header, logs the event idempotently, and "
                    + "best-effort reconciles the matching payment's status.")
    public ResponseEntity<Void> razorpay(@RequestBody String rawBody,
                                          @RequestHeader("X-Razorpay-Signature") String signature) {
        paymentWebhookService.handleRazorpayEvent(rawBody, signature);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/stripe")
    @Operation(summary = "Receive a Stripe payment webhook event",
            description = "Verifies the Stripe-Signature header, logs the event idempotently, and "
                    + "best-effort reconciles the matching payment's status.")
    public ResponseEntity<Void> stripe(@RequestBody String rawBody,
                                        @RequestHeader("Stripe-Signature") String signature) {
        paymentWebhookService.handleStripeEvent(rawBody, signature);
        return ResponseEntity.ok().build();
    }
}
