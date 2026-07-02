package com.salofresh.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload used to confirm/capture a gateway-initiated payment (UPI, credit/debit card,
 * Razorpay, Stripe). Both fields are ignored for CASH payments, which are confirmed
 * without gateway verification.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmPaymentRequest {

    private String gatewayPaymentId;
    private String gatewaySignature;
}
