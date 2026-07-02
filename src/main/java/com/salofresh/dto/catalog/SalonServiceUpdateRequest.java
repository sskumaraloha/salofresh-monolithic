package com.salofresh.dto.catalog;

import com.salofresh.common.enums.EntityStatus;
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
public class SalonServiceUpdateRequest {

    @NotNull(message = "Category is required")
    private Long categoryId;

    @NotBlank(message = "Name is required")
    @Size(max = 150, message = "Name must not exceed 150 characters")
    private String name;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Duration must be at least 1 minute")
    private Integer durationMinutes;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Price must not be negative")
    private BigDecimal price;

    @DecimalMin(value = "0.0", inclusive = true, message = "Discount price must not be negative")
    private BigDecimal discountPrice;

    @NotNull(message = "Tax percentage is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Tax percentage must not be negative")
    private BigDecimal taxPercentage;

    private String imageUrl;

    private EntityStatus status;
}
