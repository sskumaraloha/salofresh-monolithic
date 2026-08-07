package com.salofresh.service.admin;

import com.salofresh.dto.admin.CreatePlatformPlanRequest;
import com.salofresh.dto.admin.PlatformPlanAdminResponse;
import com.salofresh.dto.admin.UpdatePlatformPlanRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Admin-only CRUD over {@link com.salofresh.entity.PlatformPlan}: platform monetization /
 * commission configuration (pricing, commission %, salon caps). Distinct from
 * {@code PlatformBillingService}, which handles salon owners subscribing to plans.
 */
public interface AdminPlatformPlanService {

    Page<PlatformPlanAdminResponse> list(Pageable pageable);

    PlatformPlanAdminResponse getById(Long id);

    PlatformPlanAdminResponse create(String createdBy, CreatePlatformPlanRequest request);

    PlatformPlanAdminResponse update(Long adminUserId, Long id, UpdatePlatformPlanRequest request);

    PlatformPlanAdminResponse deactivate(Long adminUserId, Long id);

    PlatformPlanAdminResponse activate(Long adminUserId, Long id);
}
