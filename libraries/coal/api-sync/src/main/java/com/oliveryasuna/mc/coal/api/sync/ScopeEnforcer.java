package com.oliveryasuna.mc.coal.api.sync;

import com.oliveryasuna.mc.coal.api.config.ConfigManager;

import java.util.List;
import java.util.Map;

/**
 * Filters a config's payload based on
 * {@link com.oliveryasuna.mc.coal.api.annotation.Sync.Scope}.
 * <p>
 * Server extracts what to push; client applies what's authoritative.
 */
public interface ScopeEnforcer {

    //==================================================
    // Methods
    //==================================================

    Map<String, Object> extractAuthoritative(ConfigManager<?> manager);

    List<String> applyAuthoritative(
            ConfigManager<?> manager,
            Map<String, Object> values
    );

}
