package com.salofresh.mapper.coupon;

import com.salofresh.dto.coupon.CouponResponse;
import com.salofresh.entity.Coupon;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CouponMapper {

    @Mapping(target = "applicableSalonId", source = "applicableSalon.id")
    @Mapping(target = "applicableSalonName", source = "applicableSalon.name")
    CouponResponse toResponse(Coupon coupon);
}
