package com.oliveryasuna.mc.ssd.display;

import java.util.OptionalInt;

/**
 * Strategy mapping an incoming redstone signal to the glyph a display should
 * show. Pure and side-effect free, so it is trivially unit-testable and shared
 * by both the server (driving the block state) and any tooling.
 */
@FunctionalInterface
public interface SignalMapping {

    /**
     * The {@link Glyphs} index to display for a redstone signal, or
     * {@linkplain OptionalInt#empty() empty} to blank the display.
     *
     * @param signal redstone signal strength, 0-15.
     */
    OptionalInt glyphIndex(int signal);

}
