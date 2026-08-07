package com.salofresh.service.impl.inventory;

import com.salofresh.common.enums.NotificationChannel;
import com.salofresh.common.enums.NotificationType;
import com.salofresh.common.enums.StockTransactionType;
import com.salofresh.dto.inventory.StockAdjustmentRequest;
import com.salofresh.dto.inventory.StockTransactionResponse;
import com.salofresh.entity.Product;
import com.salofresh.entity.Salon;
import com.salofresh.entity.StockTransaction;
import com.salofresh.entity.User;
import com.salofresh.exception.BadRequestException;
import com.salofresh.exception.ResourceNotFoundException;
import com.salofresh.mapper.inventory.StockTransactionMapper;
import com.salofresh.notification.NotificationService;
import com.salofresh.repository.ProductRepository;
import com.salofresh.repository.StockTransactionRepository;
import com.salofresh.repository.UserRepository;
import com.salofresh.response.PagedResponse;
import com.salofresh.security.SecurityUtils;
import com.salofresh.service.inventory.StockTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StockTransactionServiceImpl implements StockTransactionService {

    private final ProductRepository productRepository;
    private final StockTransactionRepository stockTransactionRepository;
    private final StockTransactionMapper stockTransactionMapper;
    private final InventoryAccessGuard inventoryAccessGuard;
    private final SecurityUtils securityUtils;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public StockTransactionResponse recordAdjustment(Long salonId, Long productId, StockAdjustmentRequest request) {
        Salon salon = inventoryAccessGuard.requireOwnedSalon(salonId);
        Product product = productRepository.findByIdAndSalonIdAndDeletedFalse(productId, salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        int requestedQuantity = request.getQuantity();
        StockTransactionType type = request.getType();
        int delta = resolveDelta(type, requestedQuantity);

        int newStock = product.getCurrentStock() + delta;
        boolean clamped = newStock < 0;
        product.setCurrentStock(Math.max(newStock, 0));
        productRepository.save(product);

        User performedBy = userRepository.findById(securityUtils.getCurrentUserId()).orElse(null);
        String reason = clamped ? appendClampNote(request.getReason()) : request.getReason();

        StockTransaction transaction = StockTransaction.builder()
                .product(product)
                .type(type)
                .quantity(requestedQuantity)
                .reason(reason)
                .performedBy(performedBy)
                .build();
        StockTransaction saved = stockTransactionRepository.save(transaction);

        if (delta < 0 && product.isBelowReorderLevel()) {
            notifyLowStock(salon, product);
        }

        return stockTransactionMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<StockTransactionResponse> listForProduct(Long salonId, Long productId, Pageable pageable) {
        inventoryAccessGuard.requireOwnedSalon(salonId);
        productRepository.findByIdAndSalonIdAndDeletedFalse(productId, salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
        Page<StockTransaction> page = stockTransactionRepository.findAllByProductIdOrderByCreatedAtDesc(productId, pageable);
        return PagedResponse.from(page, stockTransactionMapper.toResponseList(page.getContent()));
    }

    /**
     * Sign convention (also documented on {@link StockAdjustmentRequest}): STOCK_IN and STOCK_OUT
     * quantities must be positive magnitudes - STOCK_IN increases stock by that amount, STOCK_OUT
     * decreases it. ADJUSTMENT quantities are signed and applied as-is (positive corrects stock
     * upward, negative corrects it downward) and must be non-zero.
     */
    private int resolveDelta(StockTransactionType type, int quantity) {
        return switch (type) {
            case STOCK_IN -> {
                if (quantity <= 0) {
                    throw new BadRequestException("STOCK_IN quantity must be a positive number");
                }
                yield quantity;
            }
            case STOCK_OUT -> {
                if (quantity <= 0) {
                    throw new BadRequestException("STOCK_OUT quantity must be a positive number");
                }
                yield -quantity;
            }
            case ADJUSTMENT -> {
                if (quantity == 0) {
                    throw new BadRequestException("ADJUSTMENT quantity must be non-zero");
                }
                yield quantity;
            }
        };
    }

    private String appendClampNote(String reason) {
        String note = "stock clamped at 0 (requested reduction exceeded available stock)";
        return (reason == null || reason.isBlank()) ? note : reason + " [" + note + "]";
    }

    private void notifyLowStock(Salon salon, Product product) {
        if (salon.getOwner() == null || salon.getOwner().getUser() == null) {
            return;
        }
        User owner = salon.getOwner().getUser();
        notificationService.createAndDispatch(owner, NotificationType.GENERIC, NotificationChannel.IN_APP,
                "Low stock alert",
                "%s at %s is low on stock (%d remaining, reorder level %d).".formatted(
                        product.getName(), salon.getName(), product.getCurrentStock(), product.getReorderLevel()),
                product.getId().toString(), "PRODUCT");
    }
}
