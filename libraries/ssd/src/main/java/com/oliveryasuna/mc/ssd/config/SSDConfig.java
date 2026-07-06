package com.oliveryasuna.mc.ssd.config;

import com.oliveryasuna.mc.coal.api.annotation.Comment;
import com.oliveryasuna.mc.coal.api.annotation.Config;
import com.oliveryasuna.mc.coal.api.annotation.Range;
import com.oliveryasuna.mc.coal.api.annotation.Widget;

/**
 * COAL-managed configuration for the Seven-Segment Display mod. Persisted as
 * JSON and editable through the COAL settings screen (e.g. via Mod Menu when
 * the YACL adapter is installed).
 */
@Config(id = "seven-segment-display", name = "seven-segment-display", format = "json", version = 1)
public final class SSDConfig {

    //==================================================
    // Fields
    //==================================================

    @Comment("Faintly draw the unlit segments on the display. When off, only lit segments are shown. (Reload your world).")
    public boolean showUnlitSegments = true;

    @Comment("Only allow the display to be disguised as full, solid blocks (no glass, slabs, etc.).")
    public boolean solidBlocksOnly = true;

    @Comment("Glow around lit segments: 0 = none, 1 = subtle, 2 = full. (Reload your world).")
    @Range(min = 0, max = 2)
    @Widget(Widget.Type.SLIDER)
    public int glowLevel = 1;

    //==================================================
    // Constructors
    //==================================================

    public SSDConfig() {
        super();
    }

}
