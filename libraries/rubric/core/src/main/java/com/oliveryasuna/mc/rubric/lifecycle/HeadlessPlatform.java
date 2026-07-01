package com.oliveryasuna.mc.rubric.lifecycle;

import com.oliveryasuna.mc.rubric.platform.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.concurrent.Executor;

final class HeadlessPlatform implements Platform {

    //==================================================
    // Fields
    //==================================================

    private final Path dir;

    //==================================================
    // Constructors
    //==================================================

    public HeadlessPlatform(final Path dir) {
        super();

        this.dir = dir;
    }

    //==================================================
    // Methods
    //==================================================

    @Override
    public Path configDir() {
        return dir;
    }

    @Override
    public Executor mainThreadExecutor() {
        return Runnable::run;
    }

    @Override
    public Logger logger(final String name) {
        return LoggerFactory.getLogger(name);
    }

    @Override
    public Environment environment() {
        return Environment.SERVER;
    }

}
