package com.salofresh.dto.platformbilling;

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
public class SubscribeToPlanRequest {

    @NotNull(message = "Plan is required")
    private Long planId;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;
}
