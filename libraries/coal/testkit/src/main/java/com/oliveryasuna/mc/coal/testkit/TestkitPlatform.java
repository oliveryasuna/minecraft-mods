package com.oliveryasuna.mc.coal.testkit;

import com.oliveryasuna.mc.coal.api.platform.Environment;
import com.oliveryasuna.mc.coal.api.platform.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.Executor;

/**
 * Minimal {@link Platform} shipped with the testkit — provides just enough to
 * satisfy {@link Platform} for provider tests. Registered via
 * {@code META-INF/services/com.oliveryasuna.mc.coal.api.platform.Platform}.
 * <p>
 * {@link #configDir()} returns a fresh temp directory per JVM run so tests
 * don't share state across gradle test workers. Providers wanting to write to
 * a specific location should override {@code newPlatform()} in the abstract
 * test class rather than relying on the testkit default.
 */
public final class TestkitPlatform implements Platform {

    //==================================================
    // Static fields
    //==================================================

    private static final Path CONFIG_DIR;

    static {
        Path dir;
        try {
            dir = java.nio.file.Files.createTempDirectory("coal-testkit-");
        } catch(final Exception e) {
            dir = Path.of("build", "testkit-config");
        }
        CONFIG_DIR = dir;
    }

    //==================================================
    // Constructors
    //==================================================

    public TestkitPlatform() {
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
        return Optional.of("coal-testkit");
    }

    @Override
    public Optional<String> loaderVersion() {
        return Optional.of("0.1.0");
    }

}
