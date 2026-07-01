package com.oliveryasuna.mc.rubric.migration;

import com.oliveryasuna.mc.rubric.value.Section;

/**
 * A single in-place transformation of a config tree during migration.
 */
@FunctionalInterface
public interface MigrationOp {

    //==================================================
    // Methods
    //==================================================

    void apply(Section section);

}
