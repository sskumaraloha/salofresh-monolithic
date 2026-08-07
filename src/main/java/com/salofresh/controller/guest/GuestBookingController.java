package com.salofresh.controller.guest;

import com.salofresh.constant.AppConstants;
import com.salofresh.dto.booking.AppointmentSummaryResponse;
import com.salofresh.dto.guest.GuestBookingRequest;
import com.salofresh.dto.guest.GuestBookingResponse;
import com.salofresh.dto.guest.GuestLookupRequest;
import com.salofresh.response.ApiResponse;
import com.salofresh.service.guest.GuestBookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public endpoints allowing a customer to book an appointment without first registering a full
 * account. See {@link com.salofresh.service.impl.guest.GuestBookingServiceImpl} for the guest
 * identity resolution/creation strategy. Both endpoints here are listed in
 * {@link com.salofresh.constant.SecurityConstants#PUBLIC_ENDPOINTS} since callers are, by
 * definition, not yet authenticated.
 */
@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/guest")
@RequiredArgsConstructor
@Tag(name = "Guest Booking", description = "Book an appointment or look up a booking without full account registration")
public class GuestBookingController {

    private final GuestBookingService guestBookingService;

    @PostMapping("/bookings")
    @Operation(summary = "Create a booking as a guest",
            description = "Resolves or creates a lightweight guest identity from the supplied name/email/phone, "
                    + "creates the booking, and returns a real access/refresh token pair so the guest can manage "
                    + "the booking afterward via the normal authenticated endpoints.")
    public ResponseEntity<ApiResponse<GuestBookingResponse>> createGuestBooking(
            @Valid @RequestBody GuestBookingRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Booking created successfully",
                guestBookingService.createGuestBookingAndAccount(request, httpRequest)));
    }

    @PostMapping("/bookings/lookup")
    @Operation(summary = "Find a guest booking",
            description = "Read-only lookup using the phone number used at booking time plus the booking number "
                    + "as a shared secret. Does not require or issue an authentication token.")
    public ResponseEntity<ApiResponse<AppointmentSummaryResponse>> lookupGuestBooking(
            @Valid @RequestBody GuestLookupRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Booking fetched successfully",
                guestBookingService.lookupGuestBooking(request)));
    }
}
