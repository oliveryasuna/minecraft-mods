package com.oliveryasuna.mc.coal.adapter.common;

import com.oliveryasuna.mc.coal.api.Format;
import com.oliveryasuna.mc.coal.api.io.BackupStrategy;
import com.oliveryasuna.mc.coal.api.io.ConfigIO;
import com.oliveryasuna.mc.coal.api.io.FileWatchService;
import com.oliveryasuna.mc.coal.api.schema.Schema;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

/**
 * File-backed {@link ConfigIO} using {@link JsonFormatAdapter}. Reads/writes
 * JSON only — {@link #supports(Format)} returns {@code true} only for
 * {@link Format#JSON}. The provider handles format substitution upstream when a
 * consumer requests TOML/JSON5.
 * <p>
 * No {@link BackupStrategy} or {@link FileWatchService} in v1 —
 * both accessors return {@link Optional#empty()}.
 */
final class JsonConfigIO implements ConfigIO {

    //==================================================
    // Fields
    //==================================================

    private final JsonFormatAdapter adapter;

    //==================================================
    // Constructors
    //==================================================

    JsonConfigIO() {
        super();

        this.adapter = new JsonFormatAdapter();
    }

    //==================================================
    // Methods
    //==================================================

    @Override
    public Optional<Map<String, Object>> read(
            final Path file,
            final Schema schema
    ) throws IOException {
        if(!Files.exists(file)) {
            return Optional.empty();
        }

        final byte[] bytes = Files.readAllBytes(file);

        return Optional.of(adapter.parse(bytes));
    }

    @Override
    public void write(
            final Path file,
            final Map<String, Object> tree,
            final Schema schema
    ) throws IOException {
        final Path parent = file.getParent();
        if(parent != null) {
            Files.createDirectories(parent);
        }

        Files.write(file, adapter.render(tree, schema));
    }

    @Override
    public boolean supports(final Format format) {
        return Format.JSON.equals(format);
    }

    @Override
    public Optional<BackupStrategy> backupStrategy() {
        return Optional.empty();
    }

    @Override
    public Optional<FileWatchService> fileWatchService() {
        return Optional.empty();
    }

}
