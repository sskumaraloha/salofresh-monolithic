package com.salofresh.dto.salon;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HolidayRequest {

    @NotNull
    private LocalDate holidayDate;

    @Size(max = 255)
    private String reason;

    private boolean recurringYearly;
}
