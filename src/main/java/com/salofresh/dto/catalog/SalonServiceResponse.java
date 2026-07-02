package com.salofresh.dto.catalog;

import com.salofresh.common.enums.EntityStatus;
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
public class SalonServiceResponse {

    private Long id;
    private Long salonId;
    private CategoryResponse category;
    private String name;
    private String description;
    private int durationMinutes;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private BigDecimal effectivePrice;
    private BigDecimal taxPercentage;
    private EntityStatus status;
    private String imageUrl;
}
