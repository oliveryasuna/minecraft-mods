package com.oliveryasuna.mc.rubric.io.file;

import com.oliveryasuna.mc.rubric.io.FileWatchService;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * {@link FileWatchService} implementation backed by {@link WatchService}.
 * Watches the parent directory of each registered file and dispatches per-file
 * callbacks on a dedicated daemon thread.
 * <p>
 * Per-file debouncing collapses event bursts (editor saves, format-on-save,
 * etc.) into a single callback inside {@link #debounceMillis} of the last fire.
 * Default is 200 ms; pick a higher value if your editor is chatty.
 * <p>
 * Callback exceptions are caught and dropped — a faulty consumer must not tear
 * down the pump.
 * <p>
 * {@link #close()} interrupts the pump and closes the underlying watch service;
 * pending events are dropped.
 */
public final class NioFileWatchService implements FileWatchService {

    //==================================================
    // Static fields
    //==================================================

    private static final long DEFAULT_DEBOUNCE_MILLIS = 200L;

    //==================================================
    // Fields
    //==================================================

    private final WatchService watchService;
    private final ConcurrentMap<Path, Set<Watched>> watchedByDir;
    private final ConcurrentMap<WatchKey, Path> dirByKey;
    private final Thread pump;
    private final long debounceMillis;

    //==================================================
    // Constructors
    //==================================================

    public NioFileWatchService(final long debounceMillis) throws IOException {
        super();

        this.watchService = FileSystems.getDefault().newWatchService();
        this.watchedByDir = new ConcurrentHashMap<>();
        this.dirByKey = new ConcurrentHashMap<>();
        this.debounceMillis = debounceMillis;
        this.pump = new Thread(this::pump, "rubric-filewatch");
        this.pump.setDaemon(true);
        this.pump.start();
    }

    public NioFileWatchService() throws IOException {
        this(DEFAULT_DEBOUNCE_MILLIS);
    }

    //==================================================
    // Methods
    //==================================================

    @Override
    public void watch(final Path path, final Runnable onChange) throws IOException {
        final Path file = path.toAbsolutePath().normalize();
        final Path directory = file.getParent();
        if(directory == null) {
            throw new IOException("Cannot watch a path with no parent directory: " + path);
        }

        final IOException[] thrown = {null};
        watchedByDir.compute(directory, (dir, existing) -> {
            Set<Watched> set = existing;
            if(set == null) {
                set = ConcurrentHashMap.newKeySet();
                try {
                    final WatchKey key = dir.register(
                            watchService,
                            StandardWatchEventKinds.ENTRY_MODIFY,
                            StandardWatchEventKinds.ENTRY_CREATE
                    );
                    dirByKey.put(key, dir);
                } catch(final IOException e) {
                    thrown[0] = e;
                    return existing;
                }
            }
            set.add(new Watched(file, onChange));
            return set;
        });
        if(thrown[0] != null) {
            throw thrown[0];
        }
    }

    @Override
    public void close() {
        pump.interrupt();
        try {
            watchService.close();
        } catch(final IOException ignored) {
            // Best effort.
        }
    }

    // Internal
    //--------------------------------------------------

    private void pump() {
        while(!Thread.currentThread().isInterrupted()) {
            final WatchKey key;
            try {
                key = watchService.take();
            } catch(final ClosedWatchServiceException | InterruptedException stopped) {
                return;
            }

            final Path directory = dirByKey.get(key);
            if(directory != null) {
                dispatch(directory, key);
            }
            key.reset();
        }
    }

    private void dispatch(final Path directory, final WatchKey key) {
        final Set<Watched> watched = watchedByDir.get(directory);
        if(watched == null) return;

        final Set<Watched> fired = new HashSet<>();
        final long now = System.currentTimeMillis();
        for(final WatchEvent<?> event : key.pollEvents()) {
            if(event.kind() == StandardWatchEventKinds.OVERFLOW) {
                continue;
            }
            if(!(event.context() instanceof final Path eventContext)) {
                continue;
            }
            final Path eventPath = directory.resolve(eventContext).toAbsolutePath().normalize();

            for(final Watched w : watched) {
                if(!fired.contains(w) && w.file.equals(eventPath) && w.shouldFire(now, debounceMillis)) {
                    fired.add(w);
                    try {
                        w.onChange.run();
                    } catch(final RuntimeException ignored) {
                        // A faulty consumer must not kill the pump.
                    }
                }
            }
        }
    }

    //==================================================
    // Nested
    //==================================================

    private static final class Watched {

        //==================================================
        // Fields
        //==================================================

        private final Path file;
        private final Runnable onChange;
        private final AtomicLong lastFired;

        //==================================================
        // Constructors
        //==================================================

        public Watched(
                final Path file,
                final Runnable onChange
        ) {
            super();

            this.file = file;
            this.onChange = onChange;
            this.lastFired = new AtomicLong(0L);
        }

        //==================================================
        // Methods
        //==================================================

        public boolean shouldFire(final long now, final long debounceMillis) {
            final long previous = lastFired.get();
            if(now - previous < debounceMillis) {
                return false;
            }
            return lastFired.compareAndSet(previous, now);
        }

    }

}
