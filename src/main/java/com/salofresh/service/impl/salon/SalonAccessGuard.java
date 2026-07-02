package com.salofresh.service.impl.salon;

import com.salofresh.entity.Salon;
import com.salofresh.exception.ForbiddenException;
import com.salofresh.exception.ResourceNotFoundException;
import com.salofresh.repository.SalonRepository;
import com.salofresh.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Shared helper used by every salon-scoped owner service (working hours, holidays, gallery,
 * documents...) to resolve a salon and verify that the currently authenticated user is the
 * owner of it before allowing any mutation.
 */
@Component
@RequiredArgsConstructor
class SalonAccessGuard {

    private final SalonRepository salonRepository;
    private final SecurityUtils securityUtils;

    Salon requireOwnedSalon(Long salonId) {
        Salon salon = salonRepository.findByIdAndDeletedFalse(salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Salon", "id", salonId));
        Long currentUserId = securityUtils.getCurrentUserId();
        if (salon.getOwner() == null || salon.getOwner().getUser() == null
                || !salon.getOwner().getUser().getId().equals(currentUserId)) {
            throw new ForbiddenException("You do not have permission to manage this salon");
        }
        return salon;
    }
}
