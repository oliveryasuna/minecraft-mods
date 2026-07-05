package com.oliveryasuna.mc.coal.api.migration;

import java.util.Collections;
import java.util.List;

public interface MigrationSpec {

    //==================================================
    // Static fields
    //==================================================

    /**
     * Singleton empty migration spec.
     */
    MigrationSpec EMPTY = Collections::emptyList;

    //==================================================
    // Static methods
    //==================================================

    /**
     * The singleton empty {@link MigrationSpec}. {@link #steps()} returns
     * {@link Collections#emptyList()}.
     */
    static MigrationSpec empty() {
        return EMPTY;
    }

    //==================================================
    // Methods
    //==================================================

    List<MigrationStep> steps();

}
