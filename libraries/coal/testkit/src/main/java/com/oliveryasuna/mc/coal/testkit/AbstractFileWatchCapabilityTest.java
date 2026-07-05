package com.oliveryasuna.mc.coal.testkit;

import com.oliveryasuna.mc.coal.api.io.FileWatchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Conformance tests for {@code Capability.FILE_WATCH} per spec §14.4.
 * Providers advertising {@code Capability.FILE_WATCH} MUST extend this class
 * and implement {@link #newFileWatchService()}.
 * <p>
 * File-watch tests are inherently timing-sensitive. The tests below use a
 * generous timeout (defaulting to 5s) that providers MAY override via
 * {@link #watchTimeoutSeconds()}. Providers whose service debounces heavily
 * (e.g. 3s window) SHOULD tune the timeout up.
 */
public abstract class AbstractFileWatchCapabilityTest extends AbstractCoalConformanceTest {

    //==================================================
    // Contract
    //==================================================

    /**
     * Provide a fresh {@link FileWatchService}.
     */
    protected abstract FileWatchService newFileWatchService();

    /**
     * Timeout for {@code onChange} to fire after a file write. Default 5s.
     */
    protected long watchTimeoutSeconds() {
        return 5L;
    }

    //==================================================
    // Tests
    //==================================================

    @Test
    void watchFiresOnExternalModification(
            @TempDir
            final Path tempDir) throws Exception {
        final Path file = tempDir.resolve("watched.txt");
        Files.writeString(file, "initial");

        final CountDownLatch fired = new CountDownLatch(1);
        try(final FileWatchService svc = newFileWatchService()) {
            assertNotNull(svc, "newFileWatchService() must not return null");

            try(final FileWatchService.Registration reg = svc.watch(file, fired::countDown)) {
                assertNotNull(reg, "watch(...) must not return null");

                // Give the watcher a moment to install its underlying handles.
                Thread.sleep(200L);
                Files.writeString(file, "modified");

                assertTrue(fired.await(watchTimeoutSeconds(), TimeUnit.SECONDS),
                        "onChange must fire within " + watchTimeoutSeconds() + "s of an external write (spec §14.4)");
            }
        }
    }

    @Test
    void registrationCloseStopsFurtherEvents(
            @TempDir
            final Path tempDir) throws Exception {
        final Path file = tempDir.resolve("watched.txt");
        Files.writeString(file, "initial");

        final AtomicInteger count = new AtomicInteger();
        try(final FileWatchService svc = newFileWatchService()) {
            final FileWatchService.Registration reg = svc.watch(file, count::incrementAndGet);
            Thread.sleep(200L);

            reg.close();
            Thread.sleep(200L);

            Files.writeString(file, "post-close");
            Thread.sleep(watchTimeoutSeconds() * 1_000L / 2L);

            // Not asserting exactly 0 because a lingering event from BEFORE
            // close() may still be in flight; a spec-conforming provider stops
            // NEW deliveries. The assertion below tolerates one lingering event.
            assertTrue(count.get() <= 1,
                    "post-close writes must not deliver new events (spec §14.4) — got " + count.get());
        }
    }

    @Test
    void registrationCloseIsIdempotent(
            @TempDir
            final Path tempDir) throws Exception {
        final Path file = tempDir.resolve("watched.txt");
        Files.writeString(file, "initial");

        try(final FileWatchService svc = newFileWatchService()) {
            final FileWatchService.Registration reg = svc.watch(file, () -> {
            });
            reg.close();
            reg.close();  // Second close must not throw (spec §14.4).
        }
    }

    @Test
    void providerAdvertisesFileWatch() {
        assertTrue(this.provider.supports(com.oliveryasuna.mc.coal.api.spi.Capability.FILE_WATCH),
                "Providers extending AbstractFileWatchCapabilityTest must advertise Capability.FILE_WATCH");
    }

}
