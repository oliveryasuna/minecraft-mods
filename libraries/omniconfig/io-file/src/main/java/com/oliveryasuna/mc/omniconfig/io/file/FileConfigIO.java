package com.oliveryasuna.mc.omniconfig.io.file;

import com.oliveryasuna.mc.omniconfig.io.BackupStrategy;
import com.oliveryasuna.mc.omniconfig.io.ConfigIO;
import com.oliveryasuna.mc.omniconfig.io.FormatAdapter;
import com.oliveryasuna.mc.omniconfig.schema.Schema;
import com.oliveryasuna.mc.omniconfig.value.ValueTree;
import com.oliveryasuna.mc.omniconifg.api.Format;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * File-system implementation of {@link ConfigIO}.
 * <p>
 * Format dispatch:
 * <ul>
 *     <li>
 *         {@link #read(Path, Schema)} uses {@code schema.format()} directly (the
 *         preferred path).
 *     </li>
 *     <li>
 *         {@link #read(Path)} falls back to a file-extension sniff
 *         ({@code .toml} / {@code .json} / {@code .json5}). Useful for tools or
 *         tests that don't carry a schema.
 *     </li>
 *     <li>
 *         {@link #write(Path, ValueTree, Schema)} keys off
 *         {@code schema.format()}.
 *     </li>
 * </ul>
 * <p>
 * Behavior:
 * <ul>
 *     <li>
 *         Missing file: returns {@link Optional#empty()}, or — if a default byte
 *         source is configured — parses those bytes and returns the result (so
 *         the loader can seed a freshly-installed mod with tuned defaults). The
 *         default bytes are <strong>not</strong> written to disk; the
 *         {@code ConfigManager} write-back path handles that on the first save.
 *     </li>
 *     <li>
 *         Adapter throws while parsing the on-disk file: the file is moved aside
 *         via the {@link BackupStrategy} and {@link Optional#empty()} is
 *         returned. The loader rebuilds defaults and writes them on the next
 *         save.
 *     </li>
 *     <li>No adapter for the resolved {@link Format}: {@link IOException}.</li>
 *     <li>
 *         Write is atomic via {@link AtomicFileWriter}; the original is unchanged
 *         if rendering or writing fails.
 *     </li>
 * </ul>
 */
public final class FileConfigIO implements ConfigIO {

    //==================================================
    // Static methods
    //==================================================

    private static Format sniff(final Path file) throws IOException {
        final String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        if(name.endsWith(".toml")) {
            return Format.TOML;
        } else if(name.endsWith(".json5")) {
            return Format.JSON5;
        } else if(name.endsWith(".json")) {
            return Format.JSON;
        }

        throw new IOException("Cannot infer config format from file name: " + file.getFileName());
    }

    //==================================================
    // Fields
    //==================================================

    private final Map<Format, FormatAdapter> adapters;
    private final BackupStrategy backup;
    private final Supplier<byte[]> defaults;

    //==================================================
    // Constructors
    //==================================================

    public FileConfigIO(final Map<Format, FormatAdapter> adapters) {
        this(adapters, new TimestampedBackupStrategy(), null);
    }

    public FileConfigIO(
            final Map<Format, FormatAdapter> adapters,
            final BackupStrategy backup
    ) {
        this(adapters, backup, null);
    }

    public FileConfigIO(
            final Map<Format, FormatAdapter> adapters,
            final BackupStrategy backup,
            final Supplier<byte[]> defaults
    ) {
        super();

        this.adapters = Map.copyOf(adapters);
        this.backup = backup;
        this.defaults = defaults;
    }

    //==================================================
    // Methods
    //==================================================

    @Override
    public Optional<ValueTree> read(final Path file) throws IOException {
        if(!Files.exists(file)) {
            return readDefaults(sniff(file));
        }
        return parseOrBackup(file, sniff(file));
    }

    @Override
    public Optional<ValueTree> read(
            final Path file,
            final Schema schema
    ) throws IOException {
        if(!Files.exists(file)) {
            return readDefaults(schema.format());
        }
        return parseOrBackup(file, schema.format());
    }

    @Override
    public void write(
            final Path file,
            final ValueTree tree,
            final Schema schema
    ) throws IOException {
        final FormatAdapter adapter = adapterFor(schema.format());
        final byte[] rendered = adapter.render(tree, schema);
        AtomicFileWriter.write(file, rendered);
    }

    // Internal
    //--------------------------------------------------

    private Optional<ValueTree> readDefaults(final Format format) throws IOException {
        if(defaults == null) {
            return Optional.empty();
        }
        final byte[] bytes = defaults.get();
        if(bytes == null || bytes.length == 0) {
            return Optional.empty();
        }
        final FormatAdapter adapter = adapterFor(format);
        try {
            return Optional.of(adapter.parse(bytes));
        } catch(final RuntimeException e) {
            throw new IOException("Default-resource bytes failed to parse for " + format, e);
        }
    }

    private Optional<ValueTree> parseOrBackup(
            final Path file,
            final Format format
    ) throws IOException {
        final FormatAdapter adapter = adapterFor(format);
        final byte[] bytes = Files.readAllBytes(file);
        try {
            return Optional.of(adapter.parse(bytes));
        } catch(final RuntimeException e) {
            backup.backup(file);
            return Optional.empty();
        }
    }

    private FormatAdapter adapterFor(final Format format) throws IOException {
        final FormatAdapter adapter = adapters.get(format);
        if(adapter == null) {
            throw new IOException("No FormatAdapter registered for " + format);
        }
        return adapter;
    }

}
