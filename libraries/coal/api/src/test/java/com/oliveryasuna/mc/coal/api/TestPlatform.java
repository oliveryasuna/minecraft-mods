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
 * Test-scope {@link Platform} used by {@code SmokeTest}. Provides just enough
 * to satisfy the interface — {@link #mainThreadExecutor()} runs tasks inline,
 * {@link #configDir()} points at {@code ./config} under the working directory.
 * <p>
 * Registered via
 * {@code META-INF/services/com.oliveryasuna.mc.coal.api.platform.Platform} in
 * {@code src/test/resources}, so {@link Coal}'s ServiceLoader discovery finds
 * it during test runs.
 */
public final class TestPlatform implements Platform {

    //==================================================
    // Constructors
    //==================================================

    public TestPlatform() {
        super();
    }

    //==================================================
    // Methods
    //==================================================

    @Override
    public Path configDir() {
        return Paths.get("config");
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
        return Optional.of("coal-test");
    }

    @Override
    public Optional<String> loaderVersion() {
        return Optional.of("0.0.0-test");
    }

}
