package com.salofresh.dto.waitlist;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JoinWaitlistRequest {

    @NotNull(message = "Salon is required")
    private Long salonId;

    private Long employeeId;

    private Long serviceId;

    @NotNull(message = "Preferred date is required")
    @FutureOrPresent(message = "Preferred date cannot be in the past")
    private LocalDate preferredDate;

    private LocalTime preferredStartTime;

    private LocalTime preferredEndTime;
}
