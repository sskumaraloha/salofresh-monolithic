package com.salofresh.dto.salon;

import com.salofresh.common.enums.SalonGenderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class SalonSummaryResponse {

    private Long id;
    private String name;
    private String slug;
    private String bannerImageUrl;
    private String city;
    private double ratingAverage;
    private int reviewCount;
    private SalonGenderType genderType;
    private BigDecimal startingPrice;
    private Double distanceKm;
}
