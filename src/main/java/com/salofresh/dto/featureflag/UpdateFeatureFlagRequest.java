package com.salofresh.dto.featureflag;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Patch-semantics update request: only non-null fields are applied by the service.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFeatureFlagRequest {

    @Size(max = 500, message = "Value must not exceed 500 characters")
    private String value;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    private Boolean active;
}
