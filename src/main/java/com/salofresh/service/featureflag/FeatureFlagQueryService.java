package com.salofresh.service.featureflag;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Cheap, read-through feature flag lookups intended for use by other parts of the codebase
 * (e.g. gating a feature in a controller or a background job) without needing to know anything
 * about how flags are persisted or cached.
 */
public interface FeatureFlagQueryService {

    /**
     * @return {@code true} only if a flag with this key exists, is active, has data type
     * BOOLEAN and its stored value is the literal string {@code "true"}. Never throws - any
     * lookup failure (missing key, wrong type, bad value) resolves to {@code false}.
     */
    boolean isEnabled(String key);

    /**
     * @return the flag's raw stored value if it exists and is active, otherwise {@code defaultValue}.
     */
    String getStringValue(String key, String defaultValue);

    /**
     * @return the flag's stored value parsed as a {@link BigDecimal} if it exists, is active,
     * and parses cleanly, otherwise {@code defaultValue}.
     */
    BigDecimal getNumberValue(String key, BigDecimal defaultValue);

    /**
     * @return every currently active flag as a simple key-to-value map, for clients that just
     * need to know "what's on right now" without any of the internal id/description metadata.
     */
    Map<String, String> getActiveFlagsMap();
}
