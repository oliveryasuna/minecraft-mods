package com.oliveryasuna.mc.omniconfig.migration;

import com.oliveryasuna.mc.omniconfig.value.Section;

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
