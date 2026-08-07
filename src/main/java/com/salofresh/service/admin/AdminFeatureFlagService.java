package com.salofresh.service.admin;

import com.salofresh.dto.featureflag.CreateFeatureFlagRequest;
import com.salofresh.dto.featureflag.FeatureFlagResponse;
import com.salofresh.dto.featureflag.UpdateFeatureFlagRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminFeatureFlagService {

    Page<FeatureFlagResponse> list(Pageable pageable);

    FeatureFlagResponse getByKey(String key);

    FeatureFlagResponse create(CreateFeatureFlagRequest request);

    FeatureFlagResponse update(String key, UpdateFeatureFlagRequest request);

    void delete(String key);
}
