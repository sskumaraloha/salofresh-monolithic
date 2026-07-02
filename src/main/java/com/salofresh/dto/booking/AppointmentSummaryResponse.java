package com.salofresh.dto.booking;

import com.salofresh.common.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentSummaryResponse {

    private Long id;
    private String bookingNumber;
    private String salonName;
    private String employeeName;
    private LocalDate appointmentDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private BookingStatus status;
    private List<String> serviceNames;
    private BigDecimal finalAmount;
}
