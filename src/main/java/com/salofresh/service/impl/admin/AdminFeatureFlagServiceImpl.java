package com.salofresh.service.impl.admin;

import com.salofresh.constant.CacheNames;
import com.salofresh.dto.featureflag.CreateFeatureFlagRequest;
import com.salofresh.dto.featureflag.FeatureFlagResponse;
import com.salofresh.dto.featureflag.UpdateFeatureFlagRequest;
import com.salofresh.entity.FeatureFlag;
import com.salofresh.exception.ConflictException;
import com.salofresh.exception.ResourceNotFoundException;
import com.salofresh.repository.FeatureFlagRepository;
import com.salofresh.service.admin.AdminFeatureFlagService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin write-side service for feature flag CRUD.
 *
 * <p><b>Delete:</b> {@code FeatureFlag} extends {@code Auditable}, which carries a soft-delete
 * {@code deleted} flag. However {@code FeatureFlagRepository} (left unmodified per task
 * constraints) exposes only {@code findByKey}, {@code findAllByActiveTrue} and
 * {@code existsByKey} - it does not implement {@code JpaSpecificationExecutor} the way e.g.
 * {@code SalonRepository} does, and has no deleted-aware finder/paging methods. Soft-deleting
 * here would leave {@code findByKey}/{@code existsByKey}/{@code findAllByActiveTrue} and the
 * plain {@code findAll(Pageable)} used in {@link #list} still returning "deleted" rows, and a
 * soft-deleted row would permanently block recreating the same key (the {@code flag_key} column
 * is unique). A hard delete via the inherited {@code JpaRepository.delete} keeps every existing
 * repository method correct as-is, which the task explicitly allows ("hard delete is fine").
 *
 * <p><b>Cache invalidation:</b> uses {@code @CacheEvict(allEntries = true)} on every write
 * rather than a single per-key eviction. Feature flag lookups
 * ({@code FeatureFlagQueryServiceImpl}) cache the {@code Optional} result of a lookup, including
 * "not found", so a flag that didn't exist yet when first queried could otherwise leave a stale
 * negative cache entry even after it's created. Flag writes are rare admin actions, so paying
 * for a full cache clear on write is a good trade for correctness.
 */
@Service
@RequiredArgsConstructor
public class AdminFeatureFlagServiceImpl implements AdminFeatureFlagService {

    private final FeatureFlagRepository featureFlagRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<FeatureFlagResponse> list(Pageable pageable) {
        return featureFlagRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public FeatureFlagResponse getByKey(String key) {
        return toResponse(findFlag(key));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheNames.FEATURE_FLAGS, allEntries = true)
    public FeatureFlagResponse create(CreateFeatureFlagRequest request) {
        if (featureFlagRepository.existsByKey(request.getKey())) {
            throw new ConflictException("Feature flag with key '%s' already exists".formatted(request.getKey()));
        }
        FeatureFlag flag = FeatureFlag.builder()
                .key(request.getKey())
                .value(request.getValue())
                .dataType(request.getDataType())
                .description(request.getDescription())
                .active(true)
                .build();
        return toResponse(featureFlagRepository.save(flag));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheNames.FEATURE_FLAGS, allEntries = true)
    public FeatureFlagResponse update(String key, UpdateFeatureFlagRequest request) {
        FeatureFlag flag = findFlag(key);
        if (request.getValue() != null) {
            flag.setValue(request.getValue());
        }
        if (request.getDescription() != null) {
            flag.setDescription(request.getDescription());
        }
        if (request.getActive() != null) {
            flag.setActive(request.getActive());
        }
        return toResponse(featureFlagRepository.save(flag));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheNames.FEATURE_FLAGS, allEntries = true)
    public void delete(String key) {
        featureFlagRepository.delete(findFlag(key));
    }

    private FeatureFlag findFlag(String key) {
        return featureFlagRepository.findByKey(key)
                .orElseThrow(() -> new ResourceNotFoundException("FeatureFlag", "key", key));
    }

    private FeatureFlagResponse toResponse(FeatureFlag flag) {
        return FeatureFlagResponse.builder()
                .id(flag.getId())
                .key(flag.getKey())
                .value(flag.getValue())
                .dataType(flag.getDataType())
                .description(flag.getDescription())
                .active(flag.isActive())
                .build();
    }
}
