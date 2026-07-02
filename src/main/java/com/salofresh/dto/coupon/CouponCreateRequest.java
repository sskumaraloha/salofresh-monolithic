package com.salofresh.dto.coupon;

import com.salofresh.common.enums.CouponType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CouponCreateRequest {

    @NotBlank(message = "Coupon code is required")
    private String code;

    @NotNull(message = "Coupon type is required")
    private CouponType type;

    @NotNull(message = "Discount value is required")
    @DecimalMin(value = "0.01", message = "Discount value must be greater than zero")
    private BigDecimal discountValue;

    private BigDecimal maxDiscountAmount;

    private BigDecimal minOrderAmount;

    private Integer usageLimit;

    private Integer usagePerUser;

    @NotNull(message = "Valid-from date is required")
    private Instant validFrom;

    @NotNull(message = "Valid-to date is required")
    private Instant validTo;

    private String description;

    private Long applicableSalonId;
}
