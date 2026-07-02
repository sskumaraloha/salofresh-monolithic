package com.salofresh.event;

import com.salofresh.entity.Salon;

public record SalonSuspendedEvent(Salon salon, String reason) {
}
