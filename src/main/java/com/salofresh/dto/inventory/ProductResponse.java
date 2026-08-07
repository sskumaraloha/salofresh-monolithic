package com.salofresh.dto.inventory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    private Long id;
    private Long salonId;
    private String name;
    private String sku;
    private String unit;
    private int currentStock;
    private int reorderLevel;
    private BigDecimal costPrice;
    private boolean active;
    private boolean belowReorderLevel;
    private Instant createdAt;
    private Instant updatedAt;
}
