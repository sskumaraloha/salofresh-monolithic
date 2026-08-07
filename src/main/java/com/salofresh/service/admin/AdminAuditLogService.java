package com.salofresh.service.admin;

import com.salofresh.dto.admin.AuditLogResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminAuditLogService {

    Page<AuditLogResponse> list(String entityName, String action, Pageable pageable);
}
