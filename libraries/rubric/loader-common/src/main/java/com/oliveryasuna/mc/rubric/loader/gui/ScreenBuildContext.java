package com.oliveryasuna.mc.rubric.loader.gui;

import com.oliveryasuna.mc.rubric.core.ConfigManager;
import com.oliveryasuna.mc.rubric.schema.SchemaEntry;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Per-screen-build state shared between the schema walker and the widget
 * emitters in a loader-side {@code ScreenProvider} implementation.
 */
public final class ScreenBuildContext {

    //==================================================
    // Fields
    //==================================================

    private final ConfigManager<?> manager;

    private final boolean showMetadataSuffixes;

    private final int defaultSliderTicks;

    private final Map<String, Object> staged = new LinkedHashMap<>();

    //==================================================
    // Constructors
    //==================================================

    public ScreenBuildContext(
            final ConfigManager<?> manager,
            final boolean showMetadataSuffixes,
            final int defaultSliderTicks
    ) {
        super();

        this.manager = Objects.requireNonNull(manager, "manager");
        this.showMetadataSuffixes = showMetadataSuffixes;
        this.defaultSliderTicks = defaultSliderTicks;
    }

    //==================================================
    // Methods
    //==================================================

    public void stage(
            final String path,
            final Object value
    ) {
        staged.put(path, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T currentOrDefault(
            final String path,
            final SchemaEntry entry,
            final T fallback
    ) {
        if(staged.containsKey(path)) {
            return (T)staged.get(path);
        }

        final Object live = entry.readFrom(manager.get());

        return live == null ? fallback : (T)live;
    }

    public void flush() throws IOException {
        for(final Map.Entry<String, Object> e : staged.entrySet()) {
            manager.set(e.getKey(), e.getValue());
        }
        manager.save();
    }

    public void flushUnchecked() {
        try {
            flush();
        } catch(final IOException io) {
            throw new UncheckedIOException(io);
        }
    }

    //==================================================
    // Getters/setters
    //==================================================

    public ConfigManager<?> getManager() {
        return manager;
    }

    public boolean isShowMetadataSuffixes() {
        return showMetadataSuffixes;
    }

    public int getDefaultSliderTicks() {
        return defaultSliderTicks;
    }

}
