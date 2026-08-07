package com.salofresh.mapper.platformbilling;

import com.salofresh.dto.platformbilling.PlatformPlanResponse;
import com.salofresh.dto.platformbilling.PlatformSubscriptionResponse;
import com.salofresh.entity.PlatformPlan;
import com.salofresh.entity.PlatformSubscription;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PlatformBillingMapper {

    @Mapping(target = "unlimitedSalons", expression = "java(platformPlan.isUnlimitedSalons())")
    PlatformPlanResponse toPlanResponse(PlatformPlan platformPlan);

    @Mapping(target = "plan", source = "plan")
    PlatformSubscriptionResponse toSubscriptionResponse(PlatformSubscription platformSubscription);
}
