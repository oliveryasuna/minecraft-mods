package com.oliveryasuna.mc.coal.api.config;

import com.oliveryasuna.mc.coal.api.migration.MigrationReport;
import com.oliveryasuna.mc.coal.api.validation.Correction;

import java.util.List;
import java.util.Optional;

public record LoadResult(
        ConfigSnapshot snapshot,
        List<Correction> corrections,
        Optional<MigrationReport> migration
) {

}
