package com.salofresh.dto.payout;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Admin request to move a payout from PENDING to PROCESSED. If {@code payoutReference} is
 * omitted, one is auto-generated.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessPayoutRequest {

    private String payoutReference;
}
