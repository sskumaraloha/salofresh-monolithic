package com.salofresh.dto.featureflag;

import com.salofresh.common.enums.FeatureFlagDataType;
import jakarta.validation.constraints.NotBlank;
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
public class CreateFeatureFlagRequest {

    @NotBlank(message = "Key is required")
    @Size(max = 100, message = "Key must not exceed 100 characters")
    private String key;

    @NotBlank(message = "Value is required")
    @Size(max = 500, message = "Value must not exceed 500 characters")
    private String value;

    @NotNull(message = "Data type is required")
    private FeatureFlagDataType dataType;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
}
