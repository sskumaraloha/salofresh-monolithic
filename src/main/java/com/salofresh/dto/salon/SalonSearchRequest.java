package com.salofresh.dto.salon;

import com.salofresh.common.enums.SalonGenderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Query-parameter bag for public salon search. Bound via {@code @ModelAttribute} on the controller.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalonSearchRequest {

    private String city;
    private Double minRating;
    private BigDecimal maxPrice;
    private SalonGenderType genderType;

    private Double latitude;
    private Double longitude;

    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int size = 20;

    @Builder.Default
    private String sortBy = "ratingAverage";

    @Builder.Default
    private String sortDirection = "DESC";
}
