package com.salofresh.service.webhook;

/**
 * Handles inbound payment gateway webhook events: verifies the request signature, persists an
 * idempotent {@code WebhookEventLog} entry, and makes a best-effort attempt to reconcile the
 * matching {@code Payment} record's status.
 */
public interface PaymentWebhookService {

    /**
     * Handles a Razorpay webhook callback.
     *
     * @param rawBody         the exact, unmodified raw request body
     * @param signatureHeader the {@code X-Razorpay-Signature} header value
     * @throws com.salofresh.exception.ForbiddenException if the signature cannot be verified
     */
    void handleRazorpayEvent(String rawBody, String signatureHeader);

    /**
     * Handles a Stripe webhook callback.
     *
     * @param rawBody         the exact, unmodified raw request body
     * @param signatureHeader the {@code Stripe-Signature} header value
     * @throws com.salofresh.exception.ForbiddenException if the signature cannot be verified
     */
    void handleStripeEvent(String rawBody, String signatureHeader);
}
