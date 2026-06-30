package com.oliveryasuna.mc.omniconfig.lifecycle;

import com.oliveryasuna.mc.omniconfig.platform.Platform;

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
    public System.Logger logger(final String name) {
        return System.getLogger(name);
    }

    @Override
    public Environment environment() {
        return Environment.SERVER;
    }

}
