package com.oliveryasuna.mc.omniconfig.lifecycle;

import com.oliveryasuna.mc.omniconfig.io.ConfigIO;
import com.oliveryasuna.mc.omniconfig.schema.Schema;
import com.oliveryasuna.mc.omniconfig.value.ValueTree;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory {@link ConfigIO} for headless lifecycle tests (no real files).
 */
final class InMemoryConfigIO implements ConfigIO {

    //==================================================
    // Fields
    //==================================================

    private final Map<Path, ValueTree> store;

    //==================================================
    // Constructors
    //==================================================

    public InMemoryConfigIO() {
        super();

        this.store = new HashMap<>();
    }

    //==================================================
    // Methods
    //==================================================

    @Override
    public Optional<ValueTree> read(final Path file) throws IOException {
        return Optional.ofNullable(store.get(file));
    }

    @Override
    public void write(
            final Path file,
            final ValueTree tree,
            final Schema schema
    ) throws IOException {
        store.put(file, tree);
    }

    void seed(
            final Path file,
            final ValueTree tree
    ) {
        store.put(file, tree);
    }

    ValueTree stored(final Path file) {
        return store.get(file);
    }

    boolean has(final Path file) {
        return store.containsKey(file);
    }

}
