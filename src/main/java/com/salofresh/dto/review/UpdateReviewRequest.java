package com.salofresh.dto.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request payload for a customer editing their own review (rating and/or comment).
 * The appointment/salon/employee association cannot be changed after creation.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateReviewRequest {

    @NotNull(message = "Salon rating is required")
    @Min(value = 1, message = "Salon rating must be between 1 and 5")
    @Max(value = 5, message = "Salon rating must be between 1 and 5")
    private Integer salonRating;

    @Min(value = 1, message = "Employee rating must be between 1 and 5")
    @Max(value = 5, message = "Employee rating must be between 1 and 5")
    private Integer employeeRating;

    @Min(value = 1, message = "Cleanliness rating must be between 1 and 5")
    @Max(value = 5, message = "Cleanliness rating must be between 1 and 5")
    private Integer cleanlinessRating;

    @Min(value = 1, message = "Service quality rating must be between 1 and 5")
    @Max(value = 5, message = "Service quality rating must be between 1 and 5")
    private Integer serviceQualityRating;

    @Min(value = 1, message = "Value for money rating must be between 1 and 5")
    @Max(value = 5, message = "Value for money rating must be between 1 and 5")
    private Integer valueForMoneyRating;

    @Size(max = 2000, message = "Comment must not exceed 2000 characters")
    private String comment;
}
