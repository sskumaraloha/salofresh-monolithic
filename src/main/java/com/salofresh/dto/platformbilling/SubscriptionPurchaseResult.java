package com.salofresh.dto.platformbilling;

import com.salofresh.common.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Result of initiating (or confirming) a platform plan purchase. {@code subscription} is
 * populated only once the underlying payment has actually succeeded (immediately for
 * CASH/WALLET, or after {@code confirm} verifies a gateway payment).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPurchaseResult {

    private Long paymentId;
    private String gatewayOrderId;
    private PaymentStatus paymentStatus;
    private PlatformSubscriptionResponse subscription;
}
