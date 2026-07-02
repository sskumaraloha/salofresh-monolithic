package com.salofresh.dto.payment;

import com.salofresh.common.enums.WalletTransactionType;
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
public class TransactionResponse {

    private Long id;
    private String referenceNumber;
    private WalletTransactionType transactionType;
    private BigDecimal amount;
    private String referenceType;
    private String description;
    private Instant createdAt;
}
