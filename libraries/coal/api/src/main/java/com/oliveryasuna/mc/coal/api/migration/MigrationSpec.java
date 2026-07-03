package com.oliveryasuna.mc.coal.api.migration;

import java.util.List;

public interface MigrationSpec {

    //==================================================
    // Static methods
    //==================================================

    static MigrationSpec empty() {
        // TODO: Implement.
    }

    //==================================================
    // Methods
    //==================================================

    List<MigrationStep> steps();

}
