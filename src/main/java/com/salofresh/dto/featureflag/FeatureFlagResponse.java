package com.salofresh.dto.featureflag;

import com.salofresh.common.enums.FeatureFlagDataType;
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
public class FeatureFlagResponse {

    private Long id;
    private String key;
    private String value;
    private FeatureFlagDataType dataType;
    private String description;
    private boolean active;
}
