package com.salofresh.event;

import com.salofresh.entity.User;

public record AccountLockedEvent(User user) {
}
