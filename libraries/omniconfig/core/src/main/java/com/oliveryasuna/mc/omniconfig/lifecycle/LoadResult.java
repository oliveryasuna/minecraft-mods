package com.oliveryasuna.mc.omniconfig.lifecycle;

import com.oliveryasuna.mc.omniconfig.migration.MigrationReport;
import com.oliveryasuna.mc.omniconfig.validation.Correction;
import com.oliveryasuna.mc.omniconfig.value.ConfigSnapshot;

import java.util.List;

/**
 * Outcome of a load/reload: the resulting snapshot plus as corrections applied.
 */
public record LoadResult(
        ConfigSnapshot snapshot,
        List<Correction> corrections,
        MigrationReport migration
) {

    //==================================================
    // Constructors
    //==================================================

    public LoadResult {
        corrections = List.copyOf(corrections);
    }

    //==================================================
    // Methods
    //==================================================

    public boolean hasCorrections() {
        return !corrections.isEmpty();
    }

    public boolean migrated() {
        return migration != null && migration.migrated();
    }

}
