package com.salofresh.dto.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Aggregate rating breakdown for a salon, exposing the overall average rating alongside the
 * per-criteria averages (cleanliness, service quality, value-for-money). Any of the average
 * fields may be {@code null} when no VISIBLE review has supplied a rating for that criterion.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalonRatingBreakdownResponse {

    private Double overallRating;
    private Double cleanlinessRating;
    private Double serviceQualityRating;
    private Double valueForMoneyRating;
    private long totalReviews;
}
