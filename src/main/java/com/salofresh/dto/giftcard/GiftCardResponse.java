package com.salofresh.dto.giftcard;

import com.salofresh.common.enums.GiftCardStatus;
import com.salofresh.common.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GiftCardResponse {

    private Long id;
    private String code;
    private BigDecimal initialAmount;
    private BigDecimal balance;
    private String recipientName;
    private Instant issuedAt;
    private LocalDate expiryDate;
    private GiftCardStatus status;

    /**
     * Populated instead of id/code/status while a gateway-method purchase is still awaiting
     * {@code POST /{paymentId}/confirm}; the gift card itself is not issued until the underlying
     * payment succeeds.
     */
    private Long paymentId;
    private PaymentStatus paymentStatus;
    private String gatewayOrderId;
}
