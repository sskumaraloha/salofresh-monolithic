package com.salofresh.service.impl.admin;

import com.salofresh.dto.admin.CreatePlatformPlanRequest;
import com.salofresh.dto.admin.PlatformPlanAdminResponse;
import com.salofresh.dto.admin.UpdatePlatformPlanRequest;
import com.salofresh.entity.AuditLog;
import com.salofresh.entity.PlatformPlan;
import com.salofresh.exception.ResourceNotFoundException;
import com.salofresh.repository.AuditLogRepository;
import com.salofresh.repository.PlatformPlanRepository;
import com.salofresh.service.admin.AdminPlatformPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminPlatformPlanServiceImpl implements AdminPlatformPlanService {

    private final PlatformPlanRepository platformPlanRepository;
    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<PlatformPlanAdminResponse> list(Pageable pageable) {
        return platformPlanRepository.findAllByOrderByIdAsc(pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PlatformPlanAdminResponse getById(Long id) {
        return toResponse(findPlan(id));
    }

    @Override
    public PlatformPlanAdminResponse create(String createdBy, CreatePlatformPlanRequest request) {
        PlatformPlan plan = PlatformPlan.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .billingCycle(request.getBillingCycle())
                .maxSalons(request.getMaxSalons())
                .commissionPercentage(request.getCommissionPercentage())
                .active(true)
                .build();
        PlatformPlan saved = platformPlanRepository.save(plan);

        auditLogRepository.save(AuditLog.builder()
                .entityName("PlatformPlan")
                .entityId(saved.getId().toString())
                .action("CREATE")
                .performedBy(createdBy)
                .oldValue(null)
                .newValue(summarize(saved))
                .build());

        return toResponse(saved);
    }

    @Override
    public PlatformPlanAdminResponse update(Long adminUserId, Long id, UpdatePlatformPlanRequest request) {
        PlatformPlan plan = findPlan(id);
        String oldValue = summarize(plan);

        if (request.getName() != null) {
            plan.setName(request.getName());
        }
        if (request.getDescription() != null) {
            plan.setDescription(request.getDescription());
        }
        if (request.getPrice() != null) {
            plan.setPrice(request.getPrice());
        }
        if (request.getBillingCycle() != null) {
            plan.setBillingCycle(request.getBillingCycle());
        }
        if (request.getMaxSalons() != null) {
            plan.setMaxSalons(request.getMaxSalons());
        }
        if (request.getCommissionPercentage() != null) {
            plan.setCommissionPercentage(request.getCommissionPercentage());
        }

        PlatformPlan saved = platformPlanRepository.save(plan);

        auditLogRepository.save(AuditLog.builder()
                .entityName("PlatformPlan")
                .entityId(id.toString())
                .action("UPDATE")
                .performedBy(adminUserId.toString())
                .oldValue(oldValue)
                .newValue(summarize(saved))
                .build());

        return toResponse(saved);
    }

    @Override
    public PlatformPlanAdminResponse deactivate(Long adminUserId, Long id) {
        return toggleActive(adminUserId, id, false, "DEACTIVATE");
    }

    @Override
    public PlatformPlanAdminResponse activate(Long adminUserId, Long id) {
        return toggleActive(adminUserId, id, true, "ACTIVATE");
    }

    private PlatformPlanAdminResponse toggleActive(Long adminUserId, Long id, boolean active, String action) {
        PlatformPlan plan = findPlan(id);
        String oldValue = summarize(plan);

        plan.setActive(active);
        PlatformPlan saved = platformPlanRepository.save(plan);

        auditLogRepository.save(AuditLog.builder()
                .entityName("PlatformPlan")
                .entityId(id.toString())
                .action(action)
                .performedBy(adminUserId.toString())
                .oldValue(oldValue)
                .newValue(summarize(saved))
                .build());

        return toResponse(saved);
    }

    private PlatformPlan findPlan(Long id) {
        return platformPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PlatformPlan", "id", id));
    }

    private String summarize(PlatformPlan plan) {
        return "name=" + plan.getName()
                + ",price=" + plan.getPrice()
                + ",billingCycle=" + plan.getBillingCycle()
                + ",maxSalons=" + plan.getMaxSalons()
                + ",commission=" + plan.getCommissionPercentage()
                + ",active=" + plan.isActive();
    }

    private PlatformPlanAdminResponse toResponse(PlatformPlan plan) {
        return PlatformPlanAdminResponse.builder()
                .id(plan.getId())
                .name(plan.getName())
                .description(plan.getDescription())
                .price(plan.getPrice())
                .billingCycle(plan.getBillingCycle())
                .maxSalons(plan.getMaxSalons())
                .commissionPercentage(plan.getCommissionPercentage())
                .active(plan.isActive())
                .build();
    }
}
