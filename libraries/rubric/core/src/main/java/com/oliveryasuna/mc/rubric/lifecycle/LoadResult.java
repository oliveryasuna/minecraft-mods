package com.oliveryasuna.mc.rubric.lifecycle;

import com.oliveryasuna.mc.rubric.migration.MigrationReport;
import com.oliveryasuna.mc.rubric.validation.Correction;
import com.oliveryasuna.mc.rubric.value.ConfigSnapshot;

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
