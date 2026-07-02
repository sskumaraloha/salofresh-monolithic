package com.salofresh.service.impl.payment;

import com.salofresh.common.enums.PaymentMethod;
import com.salofresh.exception.PaymentException;
import com.salofresh.service.payment.PaymentGateway;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Resolves the correct {@link PaymentGateway} implementation for a given payment method.
 * UPI/CREDIT_CARD/DEBIT_CARD/RAZORPAY are all routed through Razorpay checkout, which is the
 * dominant gateway for these methods in the Indian market; STRIPE is routed separately.
 */
@Component
public class PaymentGatewayFactory {

    private final Map<PaymentMethod, PaymentGateway> gatewaysByMethod;

    public PaymentGatewayFactory(RazorpayPaymentGatewayImpl razorpayGateway, StripePaymentGatewayImpl stripeGateway) {
        this.gatewaysByMethod = Map.of(
                PaymentMethod.RAZORPAY, razorpayGateway,
                PaymentMethod.UPI, razorpayGateway,
                PaymentMethod.CREDIT_CARD, razorpayGateway,
                PaymentMethod.DEBIT_CARD, razorpayGateway,
                PaymentMethod.STRIPE, stripeGateway
        );
    }

    public PaymentGateway getGateway(PaymentMethod method) {
        PaymentGateway gateway = gatewaysByMethod.get(method);
        if (gateway == null) {
            throw new PaymentException("No payment gateway is configured for method " + method);
        }
        return gateway;
    }
}
