package com.salofresh.dto.fraud;

import com.salofresh.common.enums.FraudAlertSeverity;
import com.salofresh.common.enums.FraudAlertStatus;
import com.salofresh.common.enums.FraudAlertType;
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
public class FraudAlertResponse {

    private Long id;
    private FraudAlertType type;
    private Long relatedUserId;
    private String relatedUserName;
    private String relatedEntityType;
    private String relatedEntityId;
    private String description;
    private FraudAlertSeverity severity;
    private FraudAlertStatus status;
    private Instant detectedAt;
    private Long resolvedById;
    private Instant resolvedAt;
    private String resolutionNote;
}
