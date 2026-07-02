package com.salofresh.dto.admin;

import com.salofresh.common.enums.SalonStatus;
import com.salofresh.common.enums.VerificationStatus;
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
public class SalonModerationResponse {

    private Long id;
    private String name;
    private String ownerName;
    private String city;
    private VerificationStatus verificationStatus;
    private SalonStatus status;
    private Instant createdAt;
}
