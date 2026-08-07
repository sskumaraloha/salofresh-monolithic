package com.salofresh.service.impl.featureflag;

import com.salofresh.common.enums.FeatureFlagDataType;
import com.salofresh.entity.FeatureFlag;
import com.salofresh.repository.FeatureFlagRepository;
import com.salofresh.service.featureflag.FeatureFlagQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeatureFlagQueryServiceImpl implements FeatureFlagQueryService {

    private final FeatureFlagCacheLoader featureFlagCacheLoader;
    private final FeatureFlagRepository featureFlagRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean isEnabled(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        try {
            return findActive(key)
                    .filter(flag -> flag.getDataType() == FeatureFlagDataType.BOOLEAN)
                    .map(flag -> Boolean.parseBoolean(flag.getValue()))
                    .orElse(false);
        } catch (Exception ex) {
            log.warn("Failed to resolve boolean feature flag '{}', treating as disabled", key, ex);
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String getStringValue(String key, String defaultValue) {
        if (key == null || key.isBlank()) {
            return defaultValue;
        }
        try {
            return findActive(key).map(FeatureFlag::getValue).orElse(defaultValue);
        } catch (Exception ex) {
            log.warn("Failed to resolve feature flag '{}', using default value", key, ex);
            return defaultValue;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getNumberValue(String key, BigDecimal defaultValue) {
        if (key == null || key.isBlank()) {
            return defaultValue;
        }
        try {
            return findActive(key)
                    .map(FeatureFlag::getValue)
                    .map(value -> parseNumber(key, value))
                    .filter(Objects::nonNull)
                    .orElse(defaultValue);
        } catch (Exception ex) {
            log.warn("Failed to resolve numeric feature flag '{}', using default value", key, ex);
            return defaultValue;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> getActiveFlagsMap() {
        Map<String, String> flags = new LinkedHashMap<>();
        featureFlagRepository.findAllByActiveTrue()
                .forEach(flag -> flags.put(flag.getKey(), flag.getValue()));
        return flags;
    }

    private Optional<FeatureFlag> findActive(String key) {
        return featureFlagCacheLoader.load(key).filter(FeatureFlag::isActive);
    }

    private BigDecimal parseNumber(String key, String value) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ex) {
            log.warn("Feature flag '{}' has a non-numeric stored value '{}'", key, value);
            return null;
        }
    }
}
