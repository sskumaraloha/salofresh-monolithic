package com.salofresh.dto.gdpr;

import com.salofresh.common.enums.DataRequestStatus;
import com.salofresh.common.enums.DataRequestType;
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
public class DataSubjectRequestResponse {

    private Long id;
    private Long userId;
    private DataRequestType requestType;
    private DataRequestStatus status;
    private Instant requestedAt;
    private Instant completedAt;
    private String exportFileUrl;
    private String rejectionReason;
}
