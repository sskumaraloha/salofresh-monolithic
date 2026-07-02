package com.salofresh.dto.payment;

import com.salofresh.common.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundResponse {

    private Long id;
    private Long paymentId;
    private BigDecimal amount;
    private String reason;
    private PaymentStatus status;
    private String gatewayRefundId;
    private Instant processedAt;
}
