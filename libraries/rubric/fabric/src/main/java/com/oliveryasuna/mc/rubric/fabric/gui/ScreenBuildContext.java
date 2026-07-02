package com.oliveryasuna.mc.rubric.fabric.gui;

import com.oliveryasuna.mc.rubric.api.ConfigManager;
import com.oliveryasuna.mc.rubric.schema.SchemaEntry;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Per-screen-build state shared between the schema walker and the widget
 * emitters in a {@link ScreenProvider} implementation. Owns:
 *
 * <ul>
 *     <li>The {@link ConfigManager} the screen edits.</li>
 *     <li>
 *         The staged-value map — user edits accumulate here until
 *         {@link #flush()} is invoked on Save &amp; Exit.
 *     </li>
 * </ul>
 * <p>
 * Passing one context around instead of a {@code (manager, staged)} pair
 * shrinks every downstream method signature by one arg.
 */
public final class ScreenBuildContext {

    //==================================================
    // Fields
    //==================================================

    private final ConfigManager<?> manager;

    /**
     * Snapshot of {@code gui.showMetadataSuffixes} taken at screen-open time.
     * Kept in the context so downstream call sites don't each re-read a static
     * config accessor.
     */
    private final boolean showMetadataSuffixes;

    /**
     * Snapshot of {@code gui.defaultSliderTicks} taken at screen-open time.
     * Consumed by {@code ScreenProviders.sliderStep} to size the discrete
     * step of continuous (double / float) slider widgets.
     */
    private final int defaultSliderTicks;

    /**
     * Insertion-ordered so the save loop honors declaration order — matters
     * only for logs / debugging, not correctness.
     */
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

    /**
     * Records a pending edit. Later {@link #flush()} pushes it through
     * {@link ConfigManager#set(String, Object)} (which validates) followed by
     * {@link ConfigManager#save()}.
     */
    public void stage(
            final String path,
            final Object value
    ) {
        staged.put(path, value);
    }

    /**
     * @return The staged value for {@code path} if the user has already edited
     * it; otherwise the live value from the manager's POJO; otherwise
     * {@code fallback} (used when the live value is {@code null}, unusual). The
     * unchecked cast is safe as long as callers pin {@code fallback}'s type to
     * the entry's declared type.
     */
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

    /**
     * Flushes every staged edit into the manager: {@code set} each path (which
     * validates), then {@code save} once. Called on Save &amp; Exit.
     */
    public void flush() throws IOException {
        for(final Map.Entry<String, Object> e : staged.entrySet()) {
            manager.set(e.getKey(), e.getValue());
        }
        manager.save();
    }

    /**
     * Convenience for {@code Runnable} save callbacks that can't declare
     * checked {@code IOException} — wraps as {@link UncheckedIOException}.
     */
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
