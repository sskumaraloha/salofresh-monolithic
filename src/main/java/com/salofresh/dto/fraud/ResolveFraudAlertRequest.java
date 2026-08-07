package com.salofresh.dto.fraud;

import com.salofresh.common.enums.FraudAlertStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResolveFraudAlertRequest {

    @NotNull(message = "Status is required")
    private FraudAlertStatus status;

    @Size(max = 1000, message = "Resolution note must not exceed 1000 characters")
    private String resolutionNote;
}
