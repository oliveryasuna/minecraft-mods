package com.oliveryasuna.mc.omniconfig.io.file;

import com.oliveryasuna.mc.omniconfig.io.BackupStrategy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Default {@link BackupStrategy}: rename the file to
 * {@code &lt;original&gt;.bak.&lt;epochMillis&gt;} in the same directory.
 */
public final class TimestampedBackupStrategy implements BackupStrategy {

    //==================================================
    // Fields
    //==================================================

    private final Clock clock;

    //==================================================
    // Constructors
    //==================================================

    TimestampedBackupStrategy(final Clock clock) {
        super();

        this.clock = clock;
    }

    public TimestampedBackupStrategy() {
        this(System::currentTimeMillis);
    }

    //==================================================
    // Methods
    //==================================================

    @Override
    public Path backup(final Path file) throws IOException {
        final Path destination = file.resolveSibling(file.getFileName() + ".bak." + clock.millis());
        Files.move(file, destination, StandardCopyOption.REPLACE_EXISTING);
        return destination;
    }

    //==================================================
    // Nested
    //==================================================

    @FunctionalInterface
    interface Clock {

        //==================================================
        // Methods
        //==================================================

        long millis();

    }

}
