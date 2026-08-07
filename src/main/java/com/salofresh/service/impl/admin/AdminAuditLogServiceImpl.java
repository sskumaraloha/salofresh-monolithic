package com.salofresh.service.impl.admin;

import com.salofresh.dto.admin.AuditLogResponse;
import com.salofresh.entity.AuditLog;
import com.salofresh.repository.AuditLogRepository;
import com.salofresh.service.admin.AdminAuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminAuditLogServiceImpl implements AdminAuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> list(String entityName, String action, Pageable pageable) {
        Page<AuditLog> page;
        if (entityName != null && action != null) {
            page = auditLogRepository.findAllByEntityNameAndActionOrderByPerformedAtDesc(entityName, action, pageable);
        } else if (entityName != null) {
            page = auditLogRepository.findAllByEntityNameOrderByPerformedAtDesc(entityName, pageable);
        } else {
            page = auditLogRepository.findAllByOrderByPerformedAtDesc(pageable);
        }
        return page.map(this::toResponse);
    }

    private AuditLogResponse toResponse(AuditLog auditLog) {
        return AuditLogResponse.builder()
                .id(auditLog.getId())
                .entityName(auditLog.getEntityName())
                .entityId(auditLog.getEntityId())
                .action(auditLog.getAction())
                .performedBy(auditLog.getPerformedBy())
                .oldValue(auditLog.getOldValue())
                .newValue(auditLog.getNewValue())
                .performedAt(auditLog.getPerformedAt())
                .build();
    }
}
