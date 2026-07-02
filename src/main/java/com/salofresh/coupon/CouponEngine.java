package com.salofresh.coupon;

import com.salofresh.common.enums.CouponType;
import com.salofresh.entity.Coupon;
import com.salofresh.entity.User;
import com.salofresh.exception.BadRequestException;
import com.salofresh.repository.CouponRepository;
import com.salofresh.repository.CouponUsageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
@RequiredArgsConstructor
public class CouponEngine {

    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;

    @Transactional(readOnly = true)
    public CouponValidationResult validateAndCalculate(String code, User user, Long salonId, BigDecimal orderAmount) {
        Coupon coupon = couponRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new BadRequestException("Invalid coupon code"));

        if (!coupon.isValidNow()) {
            throw new BadRequestException("This coupon is expired or no longer active");
        }
        if (coupon.getApplicableSalon() != null && !coupon.getApplicableSalon().getId().equals(salonId)) {
            throw new BadRequestException("This coupon is not applicable to the selected salon");
        }
        if (orderAmount.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new BadRequestException("Minimum order amount of %s is required to use this coupon"
                    .formatted(coupon.getMinOrderAmount()));
        }
        if (coupon.getUsagePerUser() != null) {
            long usedByUser = couponUsageRepository.countByCouponIdAndUserId(coupon.getId(), user.getId());
            if (usedByUser >= coupon.getUsagePerUser()) {
                throw new BadRequestException("You have already used this coupon the maximum number of times");
            }
        }

        BigDecimal discount = calculateDiscount(coupon, orderAmount);
        return new CouponValidationResult(coupon, discount);
    }

    private BigDecimal calculateDiscount(Coupon coupon, BigDecimal orderAmount) {
        BigDecimal discount;
        if (coupon.getType() == CouponType.PERCENTAGE) {
            discount = orderAmount.multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            if (coupon.getMaxDiscountAmount() != null && discount.compareTo(coupon.getMaxDiscountAmount()) > 0) {
                discount = coupon.getMaxDiscountAmount();
            }
        } else {
            discount = coupon.getDiscountValue();
        }
        if (discount.compareTo(orderAmount) > 0) {
            discount = orderAmount;
        }
        return discount;
    }
}
