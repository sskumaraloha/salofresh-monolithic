package com.salofresh.service.booking;

import com.salofresh.common.enums.BookingStatus;
import com.salofresh.dto.booking.AppointmentResponse;
import com.salofresh.dto.booking.AppointmentSummaryResponse;
import com.salofresh.dto.booking.CancelRequest;
import com.salofresh.dto.booking.CreateBookingRequest;
import com.salofresh.dto.booking.RescheduleRequest;
import com.salofresh.response.PagedResponse;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface BookingService {

    AppointmentResponse createBooking(CreateBookingRequest request);

    /**
     * Looks up a booking by either its numeric database id or its human-readable booking number.
     */
    AppointmentResponse getById(String idOrBookingNumber);

    PagedResponse<AppointmentSummaryResponse> listForCustomer(BookingStatus status, LocalDate from, LocalDate to, Pageable pageable);

    PagedResponse<AppointmentSummaryResponse> listForSalon(Long salonId, BookingStatus status, LocalDate from, LocalDate to, Pageable pageable);

    AppointmentResponse reschedule(Long appointmentId, RescheduleRequest request);

    AppointmentResponse cancel(Long appointmentId, CancelRequest request);

    AppointmentResponse confirmBySalon(Long salonId, Long appointmentId);

    AppointmentResponse rejectBySalon(Long salonId, Long appointmentId, CancelRequest request);

    AppointmentResponse markCompleted(Long salonId, Long appointmentId);

    AppointmentResponse markNoShow(Long salonId, Long appointmentId);

    /**
     * Convenience method that pre-fills a new booking request from a previously completed booking,
     * ready for the client to review/adjust and resubmit via createBooking. Does not itself create
     * a new appointment.
     */
    CreateBookingRequest rebook(Long appointmentId);
}
