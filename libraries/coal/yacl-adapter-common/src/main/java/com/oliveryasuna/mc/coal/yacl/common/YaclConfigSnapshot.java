package com.oliveryasuna.mc.coal.yacl.common;

import com.oliveryasuna.mc.coal.api.config.ConfigSnapshot;
import com.oliveryasuna.mc.coal.api.schema.Schema;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

/**
 * Immutable snapshot. Reads values off a deep-copied state instance produced
 * by {@link YaclConfigManager#snapshot()}.
 */
final class YaclConfigSnapshot implements ConfigSnapshot {

    //==================================================
    // Fields
    //==================================================

    private final Instant capturedAt;
    private final Schema schema;
    private final Object frozenState;

    //==================================================
    // Constructors
    //==================================================

    YaclConfigSnapshot(
            final Instant capturedAt,
            final Schema schema,
            final Object frozenState
    ) {
        super();

        this.capturedAt = capturedAt;
        this.schema = schema;
        this.frozenState = frozenState;
    }

    //==================================================
    // Methods
    //==================================================

    // ConfigSnapshot
    //--------------------------------------------------

    @Override
    public Instant capturedAt() {
        return capturedAt;
    }

    @Override
    public Schema schema() {
        return schema;
    }

    @Override
    public <T> Optional<T> get(
            final String dottedPath,
            final Class<T> type
    ) {
        final Object raw = getRaw(dottedPath);
        if(raw == null) {
            return Optional.empty();
        } else if(!type.isInstance(raw)) {
            return Optional.empty();
        }

        return Optional.of(type.cast(raw));
    }

    @Override
    public Object getRaw(final String dottedPath) {
        return schema.find(dottedPath)
                .map(e -> e.readFrom(frozenState))
                .orElse(null);
    }

    @Override
    public Set<String> paths() {
        return schema.paths();
    }

    @Override
    public boolean isPresent(final String dottedPath) {
        return schema.paths().contains(dottedPath);
    }

}
