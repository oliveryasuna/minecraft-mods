package com.oliveryasuna.mc.omniconfig.io;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Watches config files for external edits and invokes a callback (debounced).
 */
public interface FileWatchService extends AutoCloseable {

    //==================================================
    // Methods
    //==================================================

    void watch(
            Path path,
            Runnable onChange
    ) throws IOException;

    @Override
    void close();

}
