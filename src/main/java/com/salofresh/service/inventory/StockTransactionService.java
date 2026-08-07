package com.salofresh.service.inventory;

import com.salofresh.dto.inventory.StockAdjustmentRequest;
import com.salofresh.dto.inventory.StockTransactionResponse;
import com.salofresh.response.PagedResponse;
import org.springframework.data.domain.Pageable;

public interface StockTransactionService {

    /**
     * Records a manual stock movement for a product owned by the given salon and persists the
     * product's new {@code currentStock} together with the {@code StockTransaction} row in the
     * same transaction. See {@link com.salofresh.dto.inventory.StockAdjustmentRequest} for the
     * quantity sign convention per transaction type.
     */
    StockTransactionResponse recordAdjustment(Long salonId, Long productId, StockAdjustmentRequest request);

    PagedResponse<StockTransactionResponse> listForProduct(Long salonId, Long productId, Pageable pageable);
}
