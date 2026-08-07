package com.salofresh.service.webhook;

/**
 * Verifies the authenticity of inbound payment gateway webhook requests by checking the
 * HMAC signature the gateway sends alongside the raw request body. This is the security
 * control that replaces JWT authentication on the public webhook endpoints, since payment
 * gateways cannot present platform credentials.
 */
public interface WebhookSignatureVerifier {

    /**
     * Verifies a Razorpay webhook signature.
     *
     * @param rawBody          the exact, unmodified raw request body as received
     * @param signatureHeader  the value of the {@code X-Razorpay-Signature} header
     * @return {@code true} if the signature is valid; {@code false} if invalid, missing,
     *         malformed, or if no webhook secret is configured
     */
    boolean verifyRazorpaySignature(String rawBody, String signatureHeader);

    /**
     * Verifies a Stripe webhook signature.
     *
     * @param rawBody          the exact, unmodified raw request body as received
     * @param signatureHeader  the value of the {@code Stripe-Signature} header, in the
     *                         {@code t=<timestamp>,v1=<hexhmac>[,v0=...]} format
     * @return {@code true} if the signature is valid; {@code false} if invalid, missing,
     *         malformed, or if no webhook secret is configured
     */
    boolean verifyStripeSignature(String rawBody, String signatureHeader);
}
