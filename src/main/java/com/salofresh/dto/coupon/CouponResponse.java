package com.salofresh.dto.coupon;

import com.salofresh.common.enums.CouponType;
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
public class CouponResponse {

    private Long id;
    private String code;
    private CouponType type;
    private BigDecimal discountValue;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minOrderAmount;
    private Integer usageLimit;
    private Integer usagePerUser;
    private int timesUsed;
    private Instant validFrom;
    private Instant validTo;
    private boolean active;
    private String description;
    private Long applicableSalonId;
    private String applicableSalonName;
}
