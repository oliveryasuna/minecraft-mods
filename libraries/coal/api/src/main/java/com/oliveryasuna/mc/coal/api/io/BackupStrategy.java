package com.oliveryasuna.mc.coal.api.io;

import java.io.IOException;
import java.nio.file.Path;

public interface BackupStrategy {

    //==================================================
    // Methods
    //==================================================

    Path backup(Path file) throws IOException;

    void prune(
            Path dir,
            String baseName,
            int retention
    ) throws IOException;

}
