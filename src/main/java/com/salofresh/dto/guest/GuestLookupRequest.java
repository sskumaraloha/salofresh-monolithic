package com.salofresh.dto.guest;

import com.salofresh.validation.ValidPhone;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * "Find my booking" request for guests who no longer have (or never stored) their access token.
 * The phone number used at booking time plus the booking number act together as a shared secret -
 * no authentication token is required for this read-only lookup.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuestLookupRequest {

    @NotBlank(message = "Phone number is required")
    @ValidPhone
    private String phone;

    @NotBlank(message = "Booking number is required")
    private String bookingNumber;
}
