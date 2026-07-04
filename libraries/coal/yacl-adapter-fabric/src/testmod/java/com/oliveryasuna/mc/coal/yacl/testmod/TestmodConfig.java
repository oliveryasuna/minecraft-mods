package com.oliveryasuna.mc.coal.yacl.testmod;

import com.oliveryasuna.mc.coal.api.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Sample config exercising every scalar-scoped feature the YACL adapter v1
 * supports plus two categories that surface v1 limitations visually:
 * {@code widgetHints} (unhonored {@code @Widget} intents) and
 * {@code complexTypes} (LIST / MAP / OBJECT placeholders).
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

    // widgetHints category — @Widget intents NOT honored by v1
    //--------------------------------------------------
    //
    // Each field pairs a @Widget hint with a value shape the v1 renderer picks
    // by inference. If v1 honored @Widget, these would look different; visual
    // diff = "same as if @Widget were absent".

    @Category("widgetHints")
    @Comment("Hint: NUMBER_FIELD. Actual: slider (because @Range is present).")
    @Range(min = 0, max = 100)
    @Widget(Widget.Type.NUMBER_FIELD)
    public int hintNumberField = 25;

    @Category("widgetHints")
    @Comment("Hint: SLIDER. Actual: field (no @Range → nothing to bound).")
    @Widget(Widget.Type.SLIDER)
    public int hintSliderNoRange = 7;

    @Category("widgetHints")
    @Comment("Hint: TEXT_FIELD. Actual: text field (already the default; hint no-ops).")
    @Widget(Widget.Type.TEXT_FIELD)
    public String hintTextField = "unchanged";

    @Category("widgetHints")
    @Comment("Hint: TOGGLE on a String. Actual: text field (hint ignored for non-boolean).")
    @Widget(Widget.Type.TOGGLE)
    public String hintToggleOnString = "not-a-toggle";

    @Category("widgetHints")
    @Comment("Hint: DROPDOWN on a String without @OneOf. Actual: text field (no source list to populate the dropdown).")
    @Widget(Widget.Type.DROPDOWN)
    public String hintDropdownNoOneOf = "free-form";

    @Category("widgetHints")
    @Comment("Hint: COLOR. Actual: plain text field (v1 skips ColorControllerBuilder wiring).")
    @Widget(Widget.Type.COLOR)
    public String hintColor = "#FF8800";

    // complexTypes category — LIST / MAP / OBJECT placeholders
    //--------------------------------------------------
    //
    // Each renders as a disabled `(edit on disk)` label in the screen. The raw
    // values still round-trip through JSON persistence — edit them in the
    // config file directly.

    @Category("complexTypes")
    @Comment("LIST placeholder. Value survives round trip; edit in the JSON file.")
    public List<String> tags = new ArrayList<>(List.of("alpha", "beta", "gamma"));

    @Category("complexTypes")
    @Comment("MAP placeholder. Value survives round trip; edit in the JSON file.")
    public Map<String, Integer> counters = new LinkedHashMap<>(Map.of("alpha", 1, "beta", 2, "gamma", 3));

    @Category("complexTypes")
    @Comment("OBJECT placeholder — a nested POJO. Displays as a disabled label; value does NOT round-trip cleanly through v1's coercion layer.")
    public NestedInfo nested = new NestedInfo();

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

    /**
     * Non-annotated nested POJO used to exercise the OBJECT placeholder path.
     * The schema reader wraps this field as a leaf {@code ValueType.Kind.OBJECT}
     * — the screen provider renders it as a disabled placeholder rather than
     * recursing into its fields.
     */
    public static final class NestedInfo {

        //==================================================
        // Fields
        //==================================================

        public String label = "nested-label";

        public int count = 3;

        //==================================================
        // Constructors
        //==================================================

        public NestedInfo() {
            super();
        }

    }

}
