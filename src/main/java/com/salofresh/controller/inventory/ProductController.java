package com.salofresh.controller.inventory;

import com.salofresh.constant.AppConstants;
import com.salofresh.dto.inventory.ProductRequest;
import com.salofresh.dto.inventory.ProductResponse;
import com.salofresh.dto.inventory.StockAdjustmentRequest;
import com.salofresh.dto.inventory.StockTransactionResponse;
import com.salofresh.response.ApiResponse;
import com.salofresh.response.PagedResponse;
import com.salofresh.service.inventory.ProductService;
import com.salofresh.service.inventory.StockTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(AppConstants.API_BASE_PATH + "/salons/{salonId}/products")
@Tag(name = "Inventory - Products", description = "Owner-managed product catalog and stock levels for a salon")
@PreAuthorize("hasRole('SALON_OWNER')")
public class ProductController {

    private final ProductService productService;
    private final StockTransactionService stockTransactionService;

    @GetMapping
    @Operation(summary = "List active products for a salon")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> list(@PathVariable Long salonId) {
        return ResponseEntity.ok(ApiResponse.success("Products fetched successfully", productService.listBySalon(salonId)));
    }

    @GetMapping("/low-stock")
    @Operation(summary = "List products currently at or below their reorder level")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> listLowStock(@PathVariable Long salonId) {
        return ResponseEntity.ok(ApiResponse.success("Low stock products fetched successfully",
                productService.listLowStock(salonId)));
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Get a single product")
    public ResponseEntity<ApiResponse<ProductResponse>> getById(@PathVariable Long salonId, @PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.success("Product fetched successfully",
                productService.getById(salonId, productId)));
    }

    @PostMapping
    @Operation(summary = "Add a new product to the salon's inventory")
    public ResponseEntity<ApiResponse<ProductResponse>> create(@PathVariable Long salonId,
                                                                @Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.create(salonId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Product created successfully", response));
    }

    @PutMapping("/{productId}")
    @Operation(summary = "Update a product")
    public ResponseEntity<ApiResponse<ProductResponse>> update(@PathVariable Long salonId, @PathVariable Long productId,
                                                                @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Product updated successfully",
                productService.update(salonId, productId, request)));
    }

    @DeleteMapping("/{productId}")
    @Operation(summary = "Soft-delete a product")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long salonId, @PathVariable Long productId) {
        productService.delete(salonId, productId);
        return ResponseEntity.ok(ApiResponse.success("Product deleted successfully"));
    }

    @PostMapping("/{productId}/stock-adjustments")
    @Operation(summary = "Record a manual stock movement (stock-in, stock-out, or correction) for a product")
    public ResponseEntity<ApiResponse<StockTransactionResponse>> recordAdjustment(
            @PathVariable Long salonId, @PathVariable Long productId,
            @Valid @RequestBody StockAdjustmentRequest request) {
        StockTransactionResponse response = stockTransactionService.recordAdjustment(salonId, productId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Stock adjustment recorded successfully", response));
    }

    @GetMapping("/{productId}/stock-transactions")
    @Operation(summary = "Paginated stock transaction history for a product, most recent first")
    public ResponseEntity<ApiResponse<PagedResponse<StockTransactionResponse>>> listStockTransactions(
            @PathVariable Long salonId, @PathVariable Long productId,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success("Stock transactions fetched successfully",
                stockTransactionService.listForProduct(salonId, productId, pageable)));
    }
}
