package com.salofresh.dto.inventory;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class ProductRequest {

    @NotBlank
    @Size(max = 150)
    private String name;

    @Size(max = 50)
    private String sku;

    @Size(max = 20)
    private String unit;

    @NotNull
    @Min(0)
    private Integer reorderLevel;

    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal costPrice;
}
