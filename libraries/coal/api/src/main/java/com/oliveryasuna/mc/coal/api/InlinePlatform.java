package com.oliveryasuna.mc.coal.api;

import com.oliveryasuna.mc.coal.api.platform.Environment;
import com.oliveryasuna.mc.coal.api.platform.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.Executor;

/**
 * Minimal package-private {@link Platform} used by {@link Coal}'s inline
 * fallback path.
 * <p>
 * {@link #configDir()} resolves to {@code ./config} under the JVM's current
 * working directory. {@link #mainThreadExecutor()} is
 * {@link Runnable#run() Runnable::run} — every scheduled task fires inline on
 * the calling thread.
 */
final class InlinePlatform implements Platform {

    //==================================================
    // Static fields
    //==================================================

    static final InlinePlatform INSTANCE = new InlinePlatform();

    private static final Path CONFIG_DIR = Paths.get("config");

    //==================================================
    // Constructors
    //==================================================

    private InlinePlatform() {
        super();
    }

    //==================================================
    // Methods
    //==================================================

    @Override
    public Path configDir() {
        return CONFIG_DIR;
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
        return Environment.CLIENT;
    }

    @Override
    public Optional<Path> gameDir() {
        return Optional.empty();
    }

    @Override
    public Optional<String> loaderName() {
        return Optional.empty();
    }

    @Override
    public Optional<String> loaderVersion() {
        return Optional.empty();
    }

}
