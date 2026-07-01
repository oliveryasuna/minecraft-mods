package com.oliveryasuna.mc.rubric.migration;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.Consumer;

/**
 * Holds migration steps keyed by target version.
 */
public final class MigrationRegistry {

    //==================================================
    // Fields
    //==================================================

    private final NavigableMap<Integer, MigrationStep> steps;

    //==================================================
    // Constructors
    //==================================================

    public MigrationRegistry() {
        super();

        this.steps = new TreeMap<>();
    }

    //==================================================
    // Methods
    //==================================================

    public MigrationRegistry add(final MigrationStep step) {
        if(steps.containsKey(step.getToVersion())) {
            throw new IllegalArgumentException("duplicate migration to version " + step.getToVersion());
        }

        steps.put(step.getToVersion(), step);
        return this;
    }

    /**
     * Fluent form: {@code registry.to(2, s -> s.renameKey("a", "b"))}.
     *
     * @param version The target version.
     * @param dsl     The DSL.
     * @return This registry.
     */
    public MigrationRegistry to(
            final int version,
            final Consumer<MigrationStep.Builder> dsl
    ) {
        final MigrationStep.Builder b = MigrationStep.to(version);
        dsl.accept(b);
        return add(b.build());
    }

    public List<MigrationStep> stepsBetween(
            final int fromExclusive,
            final int toInclusive
    ) {
        return new ArrayList<>(steps.subMap(fromExclusive, false, toInclusive, true).values());
    }

    public boolean isEmpty() {
        return steps.isEmpty();
    }

}
