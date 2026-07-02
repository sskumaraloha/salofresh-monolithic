package com.salofresh.entity;

import com.salofresh.audit.Auditable;
import com.salofresh.common.enums.SalonGenderType;
import com.salofresh.common.enums.SalonStatus;
import com.salofresh.common.enums.VerificationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalTime;

@Entity
@Table(name = "salons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(callSuper = true, exclude = "owner")
public class Salon extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private SalonOwner owner;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, unique = true, length = 180)
    private String slug;

    @Column(length = 2000)
    private String description;

    @Column(name = "address_line1", nullable = false, length = 255)
    private String addressLine1;

    @Column(name = "address_line2", length = 255)
    private String addressLine2;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id", nullable = false)
    private City city;

    @Column(name = "pin_code", length = 10)
    private String pinCode;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "google_map_url", length = 500)
    private String googleMapUrl;

    @Column(name = "contact_number", nullable = false, length = 15)
    private String contactNumber;

    @Column(name = "whatsapp_number", length = 15)
    private String whatsappNumber;

    @Column(length = 150)
    private String email;

    @Column(length = 255)
    private String website;

    @Column(name = "opening_time", nullable = false)
    private LocalTime openingTime;

    @Column(name = "closing_time", nullable = false)
    private LocalTime closingTime;

    @Column(name = "parking_available", nullable = false)
    @Builder.Default
    private boolean parkingAvailable = false;

    @Column(name = "wifi_available", nullable = false)
    @Builder.Default
    private boolean wifiAvailable = false;

    @Column(name = "ac_available", nullable = false)
    @Builder.Default
    private boolean acAvailable = false;

    @Column(name = "waiting_area", nullable = false)
    @Builder.Default
    private boolean waitingArea = false;

    @Column(name = "kids_friendly", nullable = false)
    @Builder.Default
    private boolean kidsFriendly = false;

    @Column(name = "wheelchair_access", nullable = false)
    @Builder.Default
    private boolean wheelchairAccess = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender_type", nullable = false, length = 20)
    private SalonGenderType genderType;

    @Column(name = "banner_image_url", length = 500)
    private String bannerImageUrl;

    @Column(name = "gst_number", length = 20)
    private String gstNumber;

    @Column(name = "business_license_number", length = 50)
    private String businessLicenseNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    @Builder.Default
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SalonStatus status = SalonStatus.ACTIVE;

    @Column(name = "rating_average", nullable = false)
    @Builder.Default
    private double ratingAverage = 0.0;

    @Column(name = "review_count", nullable = false)
    @Builder.Default
    private int reviewCount = 0;

    @Column(name = "slot_duration_minutes", nullable = false)
    @Builder.Default
    private int slotDurationMinutes = 30;

    @Column(name = "buffer_time_minutes", nullable = false)
    @Builder.Default
    private int bufferTimeMinutes = 5;

    @Column(name = "max_bookings_per_slot", nullable = false)
    @Builder.Default
    private int maxBookingsPerSlot = 1;
}
