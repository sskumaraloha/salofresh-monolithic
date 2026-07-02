package com.salofresh.dto.review;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request payload for any authenticated user reporting a review as inappropriate.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportReviewRequest {

    @NotBlank(message = "Report reason is required")
    @Size(max = 500, message = "Reason must not exceed 500 characters")
    private String reason;
}
