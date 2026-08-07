package com.salofresh.dto.inventory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceProductUsageResponse {

    private Long id;
    private Long serviceId;
    private Long productId;
    private String productName;
    private int quantityPerService;
}
