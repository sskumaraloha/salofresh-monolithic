package com.salofresh.event;

import com.salofresh.entity.Salon;

public record SalonRejectedEvent(Salon salon, String reason) {
}
