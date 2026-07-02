package com.salofresh.event;

import com.salofresh.entity.RewardPoint;
import com.salofresh.entity.User;

public record RewardPointsEarnedEvent(User user, RewardPoint rewardPoint) {
}
