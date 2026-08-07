package com.salofresh.service.impl.featureflag;

import com.salofresh.constant.CacheNames;
import com.salofresh.entity.FeatureFlag;
import com.salofresh.repository.FeatureFlagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Internal cache-loading bean backing {@link FeatureFlagQueryServiceImpl}.
 *
 * <p>This project already wires a Caffeine-backed {@code CacheManager} with
 * {@code @EnableCaching} (see {@code com.salofresh.config.CacheConfig}), and existing services
 * (e.g. {@code CategoryServiceImpl}, {@code SalonServiceImpl}, {@code LocationServiceImpl}) use
 * plain {@code @Cacheable(cacheNames = CacheNames.XXX)} on the repository-backed lookup method.
 * We follow that exact convention rather than a hand-rolled {@code ConcurrentHashMap}.
 *
 * <p>The fetch is kept on its own small bean (instead of a private method on the query service
 * itself) because Spring's proxy-based caching only intercepts calls that go back in through the
 * bean's proxy - same-class self-invocation (e.g. {@code this.load(key)} called from another
 * method in the same class) would silently skip the cache entirely.
 */
@Component
@RequiredArgsConstructor
class FeatureFlagCacheLoader {

    private final FeatureFlagRepository featureFlagRepository;

    @Cacheable(cacheNames = CacheNames.FEATURE_FLAGS, key = "#key")
    @Transactional(readOnly = true)
    public Optional<FeatureFlag> load(String key) {
        return featureFlagRepository.findByKey(key);
    }
}
