package com.salofresh.dto.salon;

import com.salofresh.common.enums.SalonGenderType;
import com.salofresh.common.enums.SalonStatus;
import com.salofresh.common.enums.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class SalonResponse {

    private Long id;
    private Long ownerId;
    private String ownerBusinessName;

    private String name;
    private String slug;
    private String description;

    private String addressLine1;
    private String addressLine2;
    private Long cityId;
    private String cityName;
    private String stateName;
    private String countryName;
    private String pinCode;
    private Double latitude;
    private Double longitude;
    private String googleMapUrl;

    private String contactNumber;
    private String whatsappNumber;
    private String email;
    private String website;

    private LocalTime openingTime;
    private LocalTime closingTime;

    private boolean parkingAvailable;
    private boolean wifiAvailable;
    private boolean acAvailable;
    private boolean waitingArea;
    private boolean kidsFriendly;
    private boolean wheelchairAccess;

    private SalonGenderType genderType;
    private String bannerImageUrl;
    private String gstNumber;
    private String businessLicenseNumber;

    private VerificationStatus verificationStatus;
    private SalonStatus status;

    private double ratingAverage;
    private int reviewCount;

    private int slotDurationMinutes;
    private int bufferTimeMinutes;
    private int maxBookingsPerSlot;

    private List<GalleryImageResponse> galleryImages;
    private List<WorkingHoursResponse> workingHours;
    private List<HolidayResponse> holidays;

    private Double distanceKm;

    private Instant createdAt;
}
