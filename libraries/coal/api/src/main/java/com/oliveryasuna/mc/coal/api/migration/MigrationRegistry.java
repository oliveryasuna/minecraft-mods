package com.oliveryasuna.mc.coal.api.migration;

public interface MigrationRegistry {

    //==================================================
    // Methods
    //==================================================

    MigrationRegistry step(
            int fromVersion,
            int toVersion,
            MigrationOp... ops
    );

    MigrationSpec build();

}
