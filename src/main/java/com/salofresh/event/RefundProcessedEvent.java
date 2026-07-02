package com.salofresh.event;

import com.salofresh.entity.Refund;

public record RefundProcessedEvent(Refund refund) {
}
