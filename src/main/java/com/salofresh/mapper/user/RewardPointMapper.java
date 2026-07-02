package com.salofresh.mapper.user;

import com.salofresh.dto.user.RewardPointResponse;
import com.salofresh.entity.RewardPoint;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RewardPointMapper {

    RewardPointResponse toResponse(RewardPoint rewardPoint);
}
