package com.salofresh.dto.user;

import com.salofresh.common.enums.RewardPointTransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RewardPointResponse {

    private Long id;
    private RewardPointTransactionType type;
    private int points;
    private int balanceAfter;
    private String description;
    private LocalDate expiryDate;
    private Instant createdAt;
}
