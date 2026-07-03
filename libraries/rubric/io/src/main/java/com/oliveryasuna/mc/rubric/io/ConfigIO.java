package com.oliveryasuna.mc.rubric.io;

import com.oliveryasuna.mc.rubric.schema.Schema;
import com.oliveryasuna.mc.rubric.value.ValueTree;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Reads and writes a config file as a {@link ValueTree}.
 * <p>
 * Implementations (in the serialization module) own atomic writes, backups, and
 * comment application driven by the {@link Schema}.
 */
public interface ConfigIO {

    //==================================================
    // Methods
    //==================================================

    Optional<ValueTree> read(Path file) throws IOException;

    /**
     * Reads a config file using a known {@link Schema} for format dispatch.
     * Implementations that key off the schema (instead of, say, the file
     * extension) should override this; the default delegates to
     * {@link #read(Path)}.
     */
    default Optional<ValueTree> read(final Path file, final Schema schema) throws IOException {
        return read(file);
    }

    void write(
            Path file,
            ValueTree tree,
            Schema schema
    ) throws IOException;

}
