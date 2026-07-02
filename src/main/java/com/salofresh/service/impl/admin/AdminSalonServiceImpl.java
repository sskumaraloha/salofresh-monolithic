package com.salofresh.service.impl.admin;

import com.salofresh.common.enums.SalonStatus;
import com.salofresh.common.enums.VerificationStatus;
import com.salofresh.dto.admin.RejectSalonRequest;
import com.salofresh.dto.admin.SalonModerationResponse;
import com.salofresh.dto.admin.SuspendSalonRequest;
import com.salofresh.entity.AuditLog;
import com.salofresh.entity.Salon;
import com.salofresh.event.SalonApprovedEvent;
import com.salofresh.event.SalonRejectedEvent;
import com.salofresh.event.SalonSuspendedEvent;
import com.salofresh.exception.ResourceNotFoundException;
import com.salofresh.repository.AuditLogRepository;
import com.salofresh.repository.SalonRepository;
import com.salofresh.security.SecurityUtils;
import com.salofresh.service.admin.AdminSalonService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminSalonServiceImpl implements AdminSalonService {

    private final SalonRepository salonRepository;
    private final AuditLogRepository auditLogRepository;
    private final SecurityUtils securityUtils;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public Page<SalonModerationResponse> listAll(VerificationStatus verificationStatus, SalonStatus status, Pageable pageable) {
        Specification<Salon> spec = buildSpecification(verificationStatus, status);
        return salonRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SalonModerationResponse> listPendingApprovals(Pageable pageable) {
        Specification<Salon> spec = buildSpecification(VerificationStatus.PENDING, null);
        return salonRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Override
    @Transactional
    public SalonModerationResponse approve(Long salonId) {
        Salon salon = findSalon(salonId);
        salon.setVerificationStatus(VerificationStatus.APPROVED);
        Salon saved = salonRepository.save(salon);

        logAction(salonId, "APPROVE", VerificationStatus.APPROVED.name(), null);
        eventPublisher.publishEvent(new SalonApprovedEvent(saved));
        return toResponse(saved);
    }

    @Override
    @Transactional
    public SalonModerationResponse reject(Long salonId, RejectSalonRequest request) {
        Salon salon = findSalon(salonId);
        salon.setVerificationStatus(VerificationStatus.REJECTED);
        Salon saved = salonRepository.save(salon);

        logAction(salonId, "REJECT", VerificationStatus.REJECTED.name(), request.getReason());
        eventPublisher.publishEvent(new SalonRejectedEvent(saved, request.getReason()));
        return toResponse(saved);
    }

    @Override
    @Transactional
    public SalonModerationResponse suspend(Long salonId, SuspendSalonRequest request) {
        Salon salon = findSalon(salonId);
        salon.setStatus(SalonStatus.SUSPENDED);
        Salon saved = salonRepository.save(salon);

        logAction(salonId, "SUSPEND", SalonStatus.SUSPENDED.name(), request.getReason());
        eventPublisher.publishEvent(new SalonSuspendedEvent(saved, request.getReason()));
        return toResponse(saved);
    }

    private Salon findSalon(Long salonId) {
        return salonRepository.findById(salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Salon", "id", salonId));
    }

    private void logAction(Long salonId, String action, String newValue, String reason) {
        Long performedBy = securityUtils.isAuthenticated() ? securityUtils.getCurrentUserId() : null;
        auditLogRepository.save(AuditLog.builder()
                .entityName("Salon")
                .entityId(String.valueOf(salonId))
                .action(action)
                .performedBy(performedBy == null ? "SYSTEM" : String.valueOf(performedBy))
                .oldValue(reason)
                .newValue(newValue)
                .build());
    }

    private Specification<Salon> buildSpecification(VerificationStatus verificationStatus, SalonStatus status) {
        List<Specification<Salon>> specs = new ArrayList<>();
        specs.add((root, query, cb) -> cb.isFalse(root.get("deleted")));

        if (verificationStatus != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("verificationStatus"), verificationStatus));
        }
        if (status != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        return specs.stream().reduce(Specification::and).orElse(null);
    }

    private SalonModerationResponse toResponse(Salon salon) {
        String ownerName = salon.getOwner() != null && salon.getOwner().getUser() != null
                ? salon.getOwner().getUser().getFullName()
                : null;
        String cityName = salon.getCity() != null ? salon.getCity().getName() : null;
        return SalonModerationResponse.builder()
                .id(salon.getId())
                .name(salon.getName())
                .ownerName(ownerName)
                .city(cityName)
                .verificationStatus(salon.getVerificationStatus())
                .status(salon.getStatus())
                .createdAt(salon.getCreatedAt())
                .build();
    }
}
