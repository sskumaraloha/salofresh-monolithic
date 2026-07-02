package com.salofresh.service.coupon;

import com.salofresh.dto.coupon.CouponCreateRequest;
import com.salofresh.dto.coupon.CouponResponse;
import com.salofresh.dto.coupon.CouponUpdateRequest;
import com.salofresh.dto.coupon.ValidateCouponRequest;
import com.salofresh.dto.coupon.ValidateCouponResponse;
import com.salofresh.response.PagedResponse;
import org.springframework.data.domain.Pageable;

public interface CouponService {

    CouponResponse create(CouponCreateRequest request);

    CouponResponse update(Long id, CouponUpdateRequest request);

    CouponResponse getById(Long id);

    PagedResponse<CouponResponse> list(Pageable pageable, boolean activeOnly);

    void deactivate(Long id);

    /**
     * Friendly preview for customers before booking: delegates to {@link com.salofresh.coupon.CouponEngine}
     * but never throws - validation failures are reported via {@code valid=false} + a message.
     */
    ValidateCouponResponse validate(ValidateCouponRequest request, Long currentUserId);
}
