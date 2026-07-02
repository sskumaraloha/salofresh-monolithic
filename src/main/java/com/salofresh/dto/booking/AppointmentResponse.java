package com.salofresh.dto.booking;

import com.salofresh.common.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentResponse {

    private Long id;
    private String bookingNumber;
    private SalonSummary salon;
    private EmployeeSummary employee;
    private CustomerSummary customer;
    private LocalDate appointmentDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private BookingStatus status;
    private List<AppointmentServiceLineItemResponse> services;
    private BigDecimal totalAmount;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private String couponCode;
    private String notes;
    private String cancelledReason;
    private Instant cancelledAt;
    private Instant completedAt;
    private Instant createdAt;
    private Instant updatedAt;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SalonSummary {
        private Long id;
        private String name;
        private String slug;
        private String addressLine1;
        private String contactNumber;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmployeeSummary {
        private Long id;
        private String fullName;
        private String designation;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerSummary {
        private Long id;
        private String fullName;
        private String email;
        private String phone;
    }
}
