package com.salofresh.service.user;

import com.salofresh.dto.booking.CreateBookingRequest;
import com.salofresh.entity.Employee;

public interface PreferredStylistService {

    void setPreferredStylist(Long userId, Long employeeId);

    /**
     * @return the customer's preferred employee, or {@code null} if none is set (or the
     * previously-preferred employee no longer exists / is deleted).
     */
    Employee getPreferredStylist(Long userId);

    void clearPreferredStylist(Long userId);

    /**
     * Convenience wrapper around the booking module's {@code BookingService.rebook(...)}. Fetches
     * the standard rebook pre-fill template and, only when that template did not already carry an
     * employeeId, enriches it with the customer's preferred stylist (if one is set). Does not
     * create a booking itself.
     */
    CreateBookingRequest quickRebook(Long userId, Long appointmentId);
}
