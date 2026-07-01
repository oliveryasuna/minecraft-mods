package com.oliveryasuna.mc.rubric.io;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Strategy for preserving the contents of a corrupt or invalid config file
 * before it is overwritten or removed. Used by {@link ConfigIO} implementations
 * that detect an unparseable file on read.
 *
 * <p>Implementations live in the {@code serialization} module (or downstream).
 * The contract is: produce a sibling artifact (typically a renamed copy) that
 * preserves the original bytes, and return its {@link Path}. The original file
 * may be removed by the implementation as part of the strategy.
 */
@FunctionalInterface
public interface BackupStrategy {

    //==================================================
    // Methods
    //==================================================

    /**
     * Back up the given file.
     *
     * @param file The original (unparseable) file. Must exist when called.
     * @return The path of the backup artifact.
     * @throws IOException If the backup operation fails.
     */
    Path backup(Path file) throws IOException;

}
