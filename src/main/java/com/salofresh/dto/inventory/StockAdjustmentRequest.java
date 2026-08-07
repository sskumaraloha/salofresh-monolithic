package com.salofresh.dto.inventory;

import com.salofresh.common.enums.StockTransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Sign convention for {@link #quantity}, enforced in {@code StockTransactionServiceImpl}:
 * <ul>
 *     <li>{@code STOCK_IN} - quantity must be a positive magnitude; increases current stock.</li>
 *     <li>{@code STOCK_OUT} - quantity must be a positive magnitude; decreases current stock
 *     (clamped at 0 if it would go negative).</li>
 *     <li>{@code ADJUSTMENT} - quantity is signed and must be non-zero; positive corrects stock
 *     upward (e.g. a recount found extra units), negative corrects it downward (e.g. wastage or
 *     breakage). Also clamped at 0 if it would go negative.</li>
 * </ul>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockAdjustmentRequest {

    @NotNull
    private StockTransactionType type;

    @NotNull
    private Integer quantity;

    @Size(max = 255)
    private String reason;
}
