package com.salofresh.mapper.membership;

import com.salofresh.dto.membership.MembershipSubscriptionResponse;
import com.salofresh.entity.MembershipSubscription;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = MembershipPlanMapper.class)
public interface MembershipSubscriptionMapper {

    @Mapping(target = "plan", source = "plan")
    MembershipSubscriptionResponse toResponse(MembershipSubscription membershipSubscription);
}
