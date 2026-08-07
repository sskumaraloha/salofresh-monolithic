package com.salofresh.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {

    private Long id;
    private String entityName;
    private String entityId;
    private String action;
    private String performedBy;
    private String oldValue;
    private String newValue;
    private Instant performedAt;
}
