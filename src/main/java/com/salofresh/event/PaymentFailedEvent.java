package com.salofresh.event;

import com.salofresh.entity.Payment;

public record PaymentFailedEvent(Payment payment) {
}
