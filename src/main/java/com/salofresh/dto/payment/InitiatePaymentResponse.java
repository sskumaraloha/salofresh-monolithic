package com.salofresh.dto.payment;

import com.salofresh.common.enums.PaymentMethod;
import com.salofresh.common.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitiatePaymentResponse {

    private Long paymentId;
    private String gatewayOrderId;
    private BigDecimal amount;
    private String currency;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
}
