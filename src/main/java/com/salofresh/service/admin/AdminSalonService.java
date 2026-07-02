package com.salofresh.service.admin;

import com.salofresh.common.enums.SalonStatus;
import com.salofresh.common.enums.VerificationStatus;
import com.salofresh.dto.admin.RejectSalonRequest;
import com.salofresh.dto.admin.SalonModerationResponse;
import com.salofresh.dto.admin.SuspendSalonRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminSalonService {

    Page<SalonModerationResponse> listAll(VerificationStatus verificationStatus, SalonStatus status, Pageable pageable);

    Page<SalonModerationResponse> listPendingApprovals(Pageable pageable);

    SalonModerationResponse approve(Long salonId);

    SalonModerationResponse reject(Long salonId, RejectSalonRequest request);

    SalonModerationResponse suspend(Long salonId, SuspendSalonRequest request);
}
