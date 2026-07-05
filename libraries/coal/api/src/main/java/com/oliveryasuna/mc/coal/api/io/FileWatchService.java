package com.oliveryasuna.mc.coal.api.io;

import java.nio.file.Path;

public interface FileWatchService extends AutoCloseable {

    //==================================================
    // Methods
    //==================================================

    Registration watch(
            Path file,
            Runnable onChange
    );

    @Override
    void close();

    //==================================================
    // Nested
    //==================================================

    interface Registration extends AutoCloseable {

        //==================================================
        // Methods
        //==================================================

        @Override
        void close();

    }

}
