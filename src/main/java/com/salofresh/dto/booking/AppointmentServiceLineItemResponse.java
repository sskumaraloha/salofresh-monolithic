package com.salofresh.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentServiceLineItemResponse {

    private Long serviceId;
    private String serviceName;
    private BigDecimal price;
    private int durationMinutes;
    private int quantity;
}
