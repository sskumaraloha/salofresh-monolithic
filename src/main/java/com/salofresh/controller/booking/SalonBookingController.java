package com.salofresh.controller.booking;

import com.salofresh.common.enums.BookingStatus;
import com.salofresh.constant.AppConstants;
import com.salofresh.dto.booking.AppointmentResponse;
import com.salofresh.dto.booking.AppointmentSummaryResponse;
import com.salofresh.dto.booking.CancelRequest;
import com.salofresh.response.ApiResponse;
import com.salofresh.response.PagedResponse;
import com.salofresh.service.booking.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/salons/{salonId}/appointments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SALON_OWNER')")
@Tag(name = "Salon Bookings", description = "Salon-owner facing booking management")
public class SalonBookingController {

    private final BookingService bookingService;

    @GetMapping
    @Operation(summary = "List bookings for a salon (owner only)")
    public ResponseEntity<ApiResponse<PagedResponse<AppointmentSummaryResponse>>> list(
            @PathVariable Long salonId,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "appointmentDate") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Bookings fetched successfully",
                bookingService.listForSalon(salonId, status, from, to, pageable)));
    }

    @PutMapping("/{id}/confirm")
    @Operation(summary = "Confirm a pending booking")
    public ResponseEntity<ApiResponse<AppointmentResponse>> confirm(@PathVariable Long salonId, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Booking confirmed successfully",
                bookingService.confirmBySalon(salonId, id)));
    }

    @PutMapping("/{id}/reject")
    @Operation(summary = "Reject a pending booking")
    public ResponseEntity<ApiResponse<AppointmentResponse>> reject(@PathVariable Long salonId, @PathVariable Long id,
                                                                     @RequestBody(required = false) CancelRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Booking rejected successfully",
                bookingService.rejectBySalon(salonId, id, request)));
    }

    @PutMapping("/{id}/complete")
    @Operation(summary = "Mark a confirmed booking as completed")
    public ResponseEntity<ApiResponse<AppointmentResponse>> complete(@PathVariable Long salonId, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Booking marked completed successfully",
                bookingService.markCompleted(salonId, id)));
    }

    @PutMapping("/{id}/no-show")
    @Operation(summary = "Mark a booking as a no-show")
    public ResponseEntity<ApiResponse<AppointmentResponse>> noShow(@PathVariable Long salonId, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Booking marked as no-show successfully",
                bookingService.markNoShow(salonId, id)));
    }
}
