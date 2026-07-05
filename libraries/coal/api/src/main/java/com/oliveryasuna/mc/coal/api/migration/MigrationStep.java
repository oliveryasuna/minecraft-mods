package com.oliveryasuna.mc.coal.api.migration;

import java.util.List;

public interface MigrationStep {

    //==================================================
    // Methods
    //==================================================

    int fromVersion();

    int toVersion();

    List<MigrationOp> ops();

}
