package com.salofresh.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.salofresh.constant.CacheNames;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                CacheNames.SALONS,
                CacheNames.SALON_DETAILS,
                CacheNames.SERVICES,
                CacheNames.CATEGORIES,
                CacheNames.CITIES,
                CacheNames.STATES,
                CacheNames.COUNTRIES,
                CacheNames.POPULAR_SALONS,
                CacheNames.FEATURE_FLAGS
        );
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(15, TimeUnit.MINUTES)
                .maximumSize(2000));
        return cacheManager;
    }
}
