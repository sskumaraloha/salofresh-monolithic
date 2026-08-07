package com.salofresh.dto.guest;

import com.salofresh.common.enums.PaymentMethod;
import com.salofresh.validation.ValidPhone;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Request payload for booking an appointment as a guest, i.e. without having previously
 * registered a full account.
 *
 * <p>Design choice: the booking-related fields below intentionally mirror
 * {@link com.salofresh.dto.booking.CreateBookingRequest} field-for-field rather than nesting/embedding
 * that DTO. This keeps the public guest API surface self-describing in OpenAPI/Swagger (a single
 * flat JSON body) and decouples the guest contract from incidental changes to the internal
 * authenticated booking DTO. {@link com.salofresh.service.impl.guest.GuestBookingServiceImpl} is
 * responsible for translating this request into a {@code CreateBookingRequest} before delegating
 * to {@code BookingService#createBooking}.</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuestBookingRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 150, message = "Name must not exceed 150 characters")
    private String guestName;

    @Email(message = "Email must be a valid email address")
    private String guestEmail;

    @NotBlank(message = "Phone number is required")
    @ValidPhone
    private String guestPhone;

    @NotNull(message = "Salon is required")
    private Long salonId;

    private Long employeeId;

    @NotNull(message = "Appointment date is required")
    @FutureOrPresent(message = "Appointment date cannot be in the past")
    private LocalDate appointmentDate;

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @NotEmpty(message = "At least one service must be selected")
    private List<Long> serviceIds;

    @Size(max = 30, message = "Coupon code is too long")
    private String couponCode;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    @Size(max = 1000, message = "Notes are too long")
    private String notes;
}
