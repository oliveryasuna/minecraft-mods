package com.oliveryasuna.mc.omniconfig.migration;

import java.util.List;

/**
 * Records the outcome of a migration attempt during load.
 */
public record MigrationReport(
        int fromVersion,
        int toVersion,
        List<Integer> appliedVersions,
        boolean migrated,
        boolean downgrade
) {

    //==================================================
    // Static methods
    //==================================================

    public static MigrationReport none(final int version) {
        return new MigrationReport(version, version, List.of(), false, false);
    }

    //==================================================
    // Constructors
    //==================================================

    public MigrationReport {
        appliedVersions = List.copyOf(appliedVersions);
    }

}
