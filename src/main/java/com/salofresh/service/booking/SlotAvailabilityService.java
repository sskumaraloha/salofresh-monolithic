package com.salofresh.service.booking;

import com.salofresh.dto.booking.AvailableSlotResponse;
import com.salofresh.entity.Employee;
import com.salofresh.entity.Salon;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Core slot generation and validation engine for salon bookings.
 */
public interface SlotAvailabilityService {

    /**
     * Computes the bookable time windows for a salon on a given date, optionally narrowed to a
     * specific employee's schedule and/or the total duration implied by a set of requested services.
     */
    List<AvailableSlotResponse> getAvailableSlots(Long salonId, LocalDate date, Long employeeId, List<Long> serviceIds);

    /**
     * Validates that the [startTime, endTime) window is currently bookable for the salon (and, when
     * given, the specific employee) on the given date. Throws SlotUnavailableException/BadRequestException
     * when it is not. {@code excludeAppointmentId} lets a reschedule ignore its own existing row when
     * checking capacity/overlap.
     */
    void ensureSlotAvailable(Salon salon, Employee employee, LocalDate date, LocalTime startTime, LocalTime endTime,
                              Long excludeAppointmentId);
}
