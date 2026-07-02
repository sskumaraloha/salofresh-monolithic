package com.salofresh.dto.payment;

import com.salofresh.common.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitiatePaymentRequest {

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;
}
