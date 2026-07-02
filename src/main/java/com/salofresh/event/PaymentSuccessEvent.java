package com.salofresh.event;

import com.salofresh.entity.Payment;

public record PaymentSuccessEvent(Payment payment) {
}
