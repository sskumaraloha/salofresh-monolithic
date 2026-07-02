package com.salofresh.event;

import com.salofresh.entity.Appointment;

public record BookingRejectedEvent(Appointment appointment) {
}
