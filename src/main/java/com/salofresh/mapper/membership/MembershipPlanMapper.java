package com.salofresh.mapper.membership;

import com.salofresh.dto.membership.MembershipPlanResponse;
import com.salofresh.entity.MembershipPlan;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MembershipPlanMapper {

    @Mapping(target = "salonId", source = "salon.id")
    @Mapping(target = "unlimited", expression = "java(membershipPlan.isUnlimited())")
    MembershipPlanResponse toResponse(MembershipPlan membershipPlan);
}
