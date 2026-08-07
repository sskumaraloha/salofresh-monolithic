package com.salofresh.dto.inventory;

import com.salofresh.common.enums.StockTransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockTransactionResponse {

    private Long id;
    private Long productId;
    private StockTransactionType type;
    private int quantity;
    private String reason;
    private String referenceType;
    private String referenceId;
    private Long performedByUserId;
    private String performedByName;
    private Instant createdAt;
}
