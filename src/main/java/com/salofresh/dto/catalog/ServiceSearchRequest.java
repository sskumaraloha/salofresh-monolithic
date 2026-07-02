package com.salofresh.dto.catalog;

import com.salofresh.constant.AppConstants;
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
public class ServiceSearchRequest {

    private Long categoryId;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Long salonId;

    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int size = 20;

    @Builder.Default
    private String sortBy = AppConstants.DEFAULT_SORT_BY;

    @Builder.Default
    private String sortDirection = AppConstants.DEFAULT_SORT_DIRECTION;
}
