package com.salofresh.controller.booking;

import com.salofresh.constant.AppConstants;
import com.salofresh.dto.booking.AppointmentResponse;
import com.salofresh.dto.booking.AppointmentSummaryResponse;
import com.salofresh.dto.booking.CancelRequest;
import com.salofresh.dto.booking.CreateBookingRequest;
import com.salofresh.dto.booking.RescheduleRequest;
import com.salofresh.common.enums.BookingStatus;
import com.salofresh.response.ApiResponse;
import com.salofresh.response.PagedResponse;
import com.salofresh.service.booking.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/appointments")
@RequiredArgsConstructor
@Tag(name = "Appointments", description = "Customer-facing booking creation and management")
public class AppointmentController {

    private final BookingService bookingService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create a new booking for the current customer")
    public ResponseEntity<ApiResponse<AppointmentResponse>> create(@Valid @RequestBody CreateBookingRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Booking created successfully", bookingService.createBooking(request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get a booking by its numeric id or booking number")
    public ResponseEntity<ApiResponse<AppointmentResponse>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Booking fetched successfully", bookingService.getById(id)));
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List bookings for the current customer")
    public ResponseEntity<ApiResponse<PagedResponse<AppointmentSummaryResponse>>> listMine(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "appointmentDate") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Bookings fetched successfully",
                bookingService.listForCustomer(status, from, to, pageable)));
    }

    @PutMapping("/{id}/reschedule")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Reschedule an existing booking to a new date/time (and optionally employee)")
    public ResponseEntity<ApiResponse<AppointmentResponse>> reschedule(@PathVariable Long id,
                                                                        @Valid @RequestBody RescheduleRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Booking rescheduled successfully",
                bookingService.reschedule(id, request)));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cancel an existing booking")
    public ResponseEntity<ApiResponse<AppointmentResponse>> cancel(@PathVariable Long id,
                                                                     @RequestBody(required = false) CancelRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Booking cancelled successfully", bookingService.cancel(id, request)));
    }

    @GetMapping("/{id}/rebook")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Pre-fill a new booking request from a prior completed booking")
    public ResponseEntity<ApiResponse<CreateBookingRequest>> rebook(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Rebook template fetched successfully", bookingService.rebook(id)));
    }
}
