package com.oliveryasuna.mc.rubric.migration;

import com.oliveryasuna.mc.rubric.value.Scalar;
import com.oliveryasuna.mc.rubric.value.ValueNode;
import com.oliveryasuna.mc.rubric.value.ValueTree;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads/stamps the schema version on a config tree and runs registered
 * migrations.
 * <p>
 * The version lives under a reserved root key ({@link #VERSION_KEY}); the
 * mapper ignores it because it is not a schema entry.
 */
public final class Migrator {

    //==================================================
    // Fields
    //==================================================

    public static final String VERSION_KEY = "rubric_config_version";

    //==================================================
    // Constructors
    //==================================================

    public Migrator() {
        super();
    }

    //==================================================
    // Methods
    //==================================================

    /**
     * Reads the stamped version, or {@code fallback} if absent/unreadable.
     *
     * @param tree     The config tree.
     * @param fallback The fallback version.
     * @return The stamped version.
     */
    public int readVersion(
            final ValueTree tree,
            final int fallback
    ) {
        final ValueNode node = tree.root().get(VERSION_KEY);
        if(node instanceof Scalar(final Object value) && value instanceof final Number n) {
            return n.intValue();
        }

        return fallback;
    }

    public void stampVersion(
            final ValueTree tree,
            final int version
    ) {
        tree.root().put(VERSION_KEY, new Scalar(version));
    }

    public MigrationReport migrate(
            final ValueTree tree,
            final int fromVersion,
            final int toVersion,
            final MigrationRegistry registry
    ) {
        if(fromVersion > toVersion) {
            return new MigrationReport(fromVersion, toVersion, List.of(), false, true);
        }
        if(fromVersion == toVersion || registry == null || registry.isEmpty()) {
            return new MigrationReport(fromVersion, toVersion, List.of(), false, false);
        }

        final List<Integer> applied = new ArrayList<>();
        for(final MigrationStep step : registry.stepsBetween(fromVersion, toVersion)) {
            step.apply(tree.root());
            applied.add(step.getToVersion());
        }

        return new MigrationReport(fromVersion, toVersion, applied, !applied.isEmpty(), false);

    }

}
