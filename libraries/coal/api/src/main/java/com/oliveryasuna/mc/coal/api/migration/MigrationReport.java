package com.oliveryasuna.mc.coal.api.migration;

import java.util.List;

public record MigrationReport(
        int fromVersion,
        int toVersion,
        List<AppliedStep> steps
) {

    //==================================================
    // Nested
    //==================================================

    public record AppliedStep(
            int fromVersion,
            int toVersion,
            int opsApplied
    ) {

    }

}
