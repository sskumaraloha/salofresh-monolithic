package com.salofresh.service.payment;

import java.math.BigDecimal;

/**
 * Abstraction over an external payment gateway (Razorpay, Stripe, ...). Implementations must
 * never let network/transport failures propagate as unchecked exceptions other than
 * {@link com.salofresh.exception.PaymentException}, since this is called from request threads.
 */
public interface PaymentGateway {

    /**
     * Creates an order/payment-intent on the gateway and returns its identifier.
     *
     * @param amount    amount in the major currency unit (e.g. rupees, not paise)
     * @param currency  ISO currency code, e.g. "INR"
     * @param receiptId a merchant-side reference id to correlate with the gateway order
     * @return the gateway-assigned order/payment-intent id
     */
    String createOrder(BigDecimal amount, String currency, String receiptId);

    /**
     * Verifies that a client-reported payment completion is genuine.
     *
     * @param orderId          the gateway order/payment-intent id returned by {@link #createOrder}
     * @param gatewayPaymentId the gateway payment id reported by the client after checkout
     * @param signature        the gateway signature reported by the client (may be unused by some gateways)
     * @return true if the payment is verified as successful
     */
    boolean verifyPayment(String orderId, String gatewayPaymentId, String signature);

    /**
     * Best-effort refund call against the gateway. Implementations should log failures rather
     * than throw, since refund bookkeeping in our ledger must not be blocked by gateway outages.
     */
    default void refund(String gatewayTransactionId, BigDecimal amount) {
        // no-op by default
    }
}
