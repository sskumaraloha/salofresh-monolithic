package com.salofresh.service.impl.inventory;

import com.salofresh.entity.Salon;
import com.salofresh.exception.ForbiddenException;
import com.salofresh.exception.ResourceNotFoundException;
import com.salofresh.repository.SalonRepository;
import com.salofresh.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Shared helper for every inventory service (products, stock transactions, service-product usage
 * links) to resolve a salon and verify that the currently authenticated user is the owning
 * {@code SalonOwner} before allowing any read or mutation of that salon's inventory.
 */
@Component
@RequiredArgsConstructor
class InventoryAccessGuard {

    private final SalonRepository salonRepository;
    private final SecurityUtils securityUtils;

    Salon requireOwnedSalon(Long salonId) {
        Salon salon = salonRepository.findByIdAndDeletedFalse(salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Salon", "id", salonId));
        Long currentUserId = securityUtils.getCurrentUserId();
        if (salon.getOwner() == null || salon.getOwner().getUser() == null
                || !salon.getOwner().getUser().getId().equals(currentUserId)) {
            throw new ForbiddenException("You do not have permission to manage this salon's inventory");
        }
        return salon;
    }
}
