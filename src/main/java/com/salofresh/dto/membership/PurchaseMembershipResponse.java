package com.salofresh.dto.membership;

import com.salofresh.common.enums.PaymentMethod;
import com.salofresh.common.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Result of initiating (or confirming) a membership plan purchase. {@code subscription} is
 * populated only once the underlying payment has actually succeeded (immediately for CASH/WALLET,
 * or after {@code confirm} verifies a gateway payment).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseMembershipResponse {

    private Long paymentId;
    private String gatewayOrderId;
    private BigDecimal amount;
    private String currency;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private MembershipSubscriptionResponse subscription;
}
