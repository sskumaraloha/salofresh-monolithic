package com.salofresh.dto.coupon;

import com.salofresh.common.enums.CouponType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * All fields are optional; only non-null fields are applied to the existing coupon.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponUpdateRequest {

    private String code;
    private CouponType type;
    private BigDecimal discountValue;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minOrderAmount;
    private Integer usageLimit;
    private Integer usagePerUser;
    private Instant validFrom;
    private Instant validTo;
    private String description;
    private Long applicableSalonId;
    private Boolean active;
}
