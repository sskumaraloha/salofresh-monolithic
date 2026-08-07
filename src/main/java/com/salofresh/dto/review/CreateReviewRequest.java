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
 * Request payload to create a review for a completed appointment.
 *
 * <p>Images are NOT attached at creation time. A review is created first (0 images), then
 * images are uploaded and attached one at a time via {@code POST /api/v1/reviews/{id}/images},
 * which stores the file via {@code FileStorageService} under the "review-images" subdirectory
 * and persists a {@code ReviewImage} row. This upload-then-attach flow avoids needing a
 * multipart/JSON hybrid payload on the create endpoint.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateReviewRequest {

    @NotNull(message = "Appointment id is required")
    private Long appointmentId;

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
