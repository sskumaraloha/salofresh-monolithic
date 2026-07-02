package com.salofresh.coupon;

import com.salofresh.entity.Coupon;

import java.math.BigDecimal;

public record CouponValidationResult(Coupon coupon, BigDecimal discountAmount) {
}
