package com.oliveryasuna.mc.coal.api.io;

import com.oliveryasuna.mc.coal.api.Format;
import com.oliveryasuna.mc.coal.api.schema.Schema;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

public interface ConfigIO {

    //==================================================
    // Methods
    //==================================================

    Optional<Map<String, Object>> read(
            Path file,
            Schema schema
    ) throws IOException;

    void write(
            Path file,
            Map<String, Object> tree,
            Schema schema
    ) throws IOException;

    boolean supports(Format format);

    Optional<BackupStrategy> backupStrategy();

    Optional<FileWatchService> fileWatchService();

}
