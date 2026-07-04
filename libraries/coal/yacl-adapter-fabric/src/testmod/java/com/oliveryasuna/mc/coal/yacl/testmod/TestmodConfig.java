package com.oliveryasuna.mc.coal.yacl.testmod;

import com.oliveryasuna.mc.coal.api.annotation.*;

/**
 * Sample config exercising every scalar-scoped feature the YACL adapter v1
 * supports: booleans, strings, all numeric primitives, {@code @Range} sliders,
 * {@code @OneOf} dropdowns, enums, and categorized entries.
 * <p>
 * Complex types (List, Map, nested objects) render as disabled placeholders in
 * v1 — see {@code YaclScreenProvider#buildPlaceholder} — so they're omitted
 * here to keep the demo screen clean.
 */
@Config(id = "coal_yacl_testmod", name = "coal_yacl_testmod", format = "json", version = 1)
public final class TestmodConfig {

    //==================================================
    // Fields
    //==================================================

    // Root category
    //--------------------------------------------------

    @Comment("Master toggle for the demo feature.")
    public boolean enabled = true;

    @Comment("Free-form label.")
    public String label = "hello coal";

    @Comment("Quality preset — @OneOf dropdown.")
    @OneOf({"low", "medium", "high"})
    public String quality = "medium";

    @Comment("Demo mode — enum dropdown.")
    public Mode mode = Mode.BALANCED;

    // Display category
    //--------------------------------------------------

    @Category("display")
    @Comment("Opacity — @Range double slider [0.0, 1.0].")
    @Range(min = 0.0, max = 1.0)
    public double opacity = 0.8;

    @Category("display")
    @Comment("Render distance — @Range int slider [2, 32].")
    @Range(min = 2, max = 32)
    public int renderDistance = 12;

    @Category("display")
    @Comment("Smoothing — @Range float slider [0.0, 2.0].")
    @Range(min = 0.0, max = 2.0)
    public float smoothing = 1.0f;

    // Audio category
    //--------------------------------------------------

    @Category("audio")
    @Comment("Master volume — @Range double slider.")
    @Range(min = 0.0, max = 1.0)
    public double masterVolume = 1.0;

    @Category("audio")
    @Comment("Music volume — @Range double slider.")
    @Range(min = 0.0, max = 1.0)
    public double musicVolume = 0.5;

    @Category("audio")
    @Comment("Free-form field — unbounded long.")
    public long bigCounter = 42L;

    //==================================================
    // Constructors
    //==================================================

    public TestmodConfig() {
        super();
    }

    //==================================================
    // Nested
    //==================================================

    public enum Mode {

        //==================================================
        // Values
        //==================================================

        PERFORMANCE,

        BALANCED,

        QUALITY

    }

}
