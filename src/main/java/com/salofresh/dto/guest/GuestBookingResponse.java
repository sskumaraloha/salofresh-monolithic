package com.salofresh.dto.guest;

import com.salofresh.dto.booking.AppointmentResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response returned after a successful guest booking. Besides the appointment details, it carries
 * a full access/refresh token pair for the guest identity that was created (or reused) so that the
 * client app can immediately call the same authenticated endpoints (GET /api/v1/appointments/{id},
 * cancel, reschedule, etc.) that a fully registered customer would use - no special-casing needed
 * anywhere else in the API.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuestBookingResponse {

    private String bookingNumber;
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresIn;
    private AppointmentResponse appointment;
}
