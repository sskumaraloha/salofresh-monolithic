package com.salofresh.dto.giftcard;

import com.salofresh.common.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class PurchaseGiftCardRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.00", message = "Gift card amount must be at least 1.00")
    private BigDecimal amount;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    @NotBlank(message = "Recipient name is required")
    @Size(max = 150, message = "Recipient name must be at most 150 characters")
    private String recipientName;

    @Email(message = "Recipient email must be a valid email address")
    @Size(max = 150, message = "Recipient email must be at most 150 characters")
    private String recipientEmail;

    @Size(max = 15, message = "Recipient phone must be at most 15 characters")
    private String recipientPhone;

    @Size(max = 500, message = "Message must be at most 500 characters")
    private String message;

    /**
     * Number of days the gift card remains valid from issuance. Defaults to 365 when omitted.
     */
    private Integer validityDays;
}
