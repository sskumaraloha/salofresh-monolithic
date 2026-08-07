package com.salofresh.service.guest;

import com.salofresh.dto.booking.AppointmentSummaryResponse;
import com.salofresh.dto.guest.GuestBookingRequest;
import com.salofresh.dto.guest.GuestBookingResponse;
import com.salofresh.dto.guest.GuestLookupRequest;
import jakarta.servlet.http.HttpServletRequest;

public interface GuestBookingService {

    /**
     * Resolves-or-creates a guest identity (User + Customer + Wallet) for the given phone number,
     * creates the booking by delegating to {@code BookingService#createBooking}, and issues a real
     * JWT access/refresh token pair for the guest so they can immediately manage their booking via
     * the normal authenticated endpoints.
     */
    GuestBookingResponse createGuestBookingAndAccount(GuestBookingRequest request, HttpServletRequest httpRequest);

    /**
     * Read-only "find my booking" lookup for guests, keyed by phone number + booking number acting
     * together as a shared secret. Does not require (or issue) any authentication token.
     */
    AppointmentSummaryResponse lookupGuestBooking(GuestLookupRequest request);
}
