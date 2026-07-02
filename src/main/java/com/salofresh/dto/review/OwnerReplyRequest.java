package com.salofresh.dto.review;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request payload for a salon owner replying to a review.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerReplyRequest {

    @NotBlank(message = "Reply text is required")
    @Size(max = 2000, message = "Reply must not exceed 2000 characters")
    private String reply;
}
