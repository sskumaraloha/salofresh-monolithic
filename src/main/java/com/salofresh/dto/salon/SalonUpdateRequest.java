package com.salofresh.dto.salon;

import com.salofresh.common.enums.SalonGenderType;
import com.salofresh.validation.ValidGst;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalonUpdateRequest {

    @NotBlank
    @Size(max = 150)
    private String name;

    @Size(max = 2000)
    private String description;

    @NotBlank
    @Size(max = 255)
    private String addressLine1;

    @Size(max = 255)
    private String addressLine2;

    @NotNull
    private Long cityId;

    @Size(max = 10)
    private String pinCode;

    @NotNull
    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    private Double latitude;

    @NotNull
    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    private Double longitude;

    @Size(max = 500)
    private String googleMapUrl;

    @NotBlank
    @Size(max = 15)
    private String contactNumber;

    @Size(max = 15)
    private String whatsappNumber;

    @Email
    @Size(max = 150)
    private String email;

    @Size(max = 255)
    private String website;

    @NotNull
    private LocalTime openingTime;

    @NotNull
    private LocalTime closingTime;

    private boolean parkingAvailable;

    private boolean wifiAvailable;

    private boolean acAvailable;

    private boolean waitingArea;

    private boolean kidsFriendly;

    private boolean wheelchairAccess;

    @NotNull
    private SalonGenderType genderType;

    @ValidGst
    private String gstNumber;

    @Size(max = 50)
    private String businessLicenseNumber;

    @Min(5)
    @Max(240)
    private Integer slotDurationMinutes;

    @Min(0)
    @Max(120)
    private Integer bufferTimeMinutes;

    @Min(1)
    @Max(20)
    private Integer maxBookingsPerSlot;
}
