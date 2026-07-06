package com.oliveryasuna.mc.coal.cloth.testmod;

import com.oliveryasuna.mc.coal.api.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Sample config exercising every rendering path the YACL adapter v1 supports.
 * Categorized so each tab in the generated screen surfaces a distinct concern:
 * <ul>
 *     <li>
 *         Root, {@code display}, {@code audio} — scalars, {@code @Range}
 *         sliders, {@code @OneOf} dropdowns, enums.
 *     </li>
 *     <li>
 *         {@code yaclFeatures} — COAL annotations wired to YACL features:
 *         {@code @RequiresRestart} / {@code @Reload(RESTART)} ->
 *         {@code OptionFlag.GAME_RESTART} indicator, {@code @Length} -> list
 *         min/max entries.
 *     </li>
 *     <li>
 *         {@code widgetHints} — {@code @Widget} intents whose prerequisites
 *         are unmet; the provider silently falls back per spec §7.14. Only
 *         {@code COLOR} is honored (via {@code ColorControllerBuilder}); the
 *         rest render as the type-inferred default.
 *     </li>
 *     <li>
 *         {@code complexTypes} — {@code LIST} renders as a real
 *         {@code ListOption} editor; {@code OBJECT} recurses into fields as a
 *         {@code complexTypes.nested} sub-category via schema-layer
 *         flattening; {@code MAP} is the one remaining placeholder (YACL has
 *         no map primitive).
 *     </li>
 * </ul>
 */
@Config(id = "coal_cloth_testmod", name = "coal_cloth_testmod", format = "json", version = 1)
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

    // yaclFeatures category — COAL annotations wired to YACL features
    //--------------------------------------------------

    @Category("yaclFeatures")
    @Comment("@RequiresRestart -> YACL renders a 'requires restart' indicator (OptionFlag.GAME_RESTART).")
    @RequiresRestart
    public boolean restartSensitiveToggle = false;

    @Category("yaclFeatures")
    @Comment("@Reload(RESTART) — same effect as @RequiresRestart via the explicit tier form.")
    @Reload(Reload.Tier.RESTART)
    public int restartSensitiveInt = 3;

    @Category("yaclFeatures")
    @Comment("@Length(min=2, max=6) -> YACL enforces list bounds (min entries not removable, max entries not addable).")
    @Length(min = 2, max = 6)
    public List<String> boundedTags = new ArrayList<>(List.of("first", "second", "third"));

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
    @Comment("Hint: SLIDER. Actual: field (no @Range -> nothing to bound).")
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
    @Comment("Hint: COLOR. Actual: color picker (wired via ColorControllerBuilder; alpha channel not exposed).")
    @Widget(Widget.Type.COLOR)
    public String hintColor = "#FF8800";

    // complexTypes category — LIST / MAP / OBJECT placeholders
    //--------------------------------------------------
    //
    // Each renders as a disabled `(edit on disk)` label in the screen. The raw
    // values still round-trip through JSON persistence — edit them in the
    // config file directly.

    @Category("complexTypes")
    @Comment("LIST editor — add/remove/reorder String entries via YACL's ListOption. Element type resolved from the parameterized field type.")
    public List<String> tags = new ArrayList<>(List.of("alpha", "beta", "gamma"));

    @Category("complexTypes")
    @Comment("MAP placeholder. Value survives round trip; edit in the JSON file.")
    public Map<String, Integer> counters = new LinkedHashMap<>(Map.of("alpha", 1, "beta", 2, "gamma", 3));

    @Category("complexTypes")
    @Comment("Nested POJO — schema-layer flattening inlines its fields as a `complexTypes.nested` sub-category with `label` (String) and `count` (int) as ordinary entries.")
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
     * Non-annotated nested POJO used to exercise the schema-layer flattening
     * path. {@code AnnotationSchemaReader#walkClass} recurses into this type
     * and inlines its fields as entries under a {@code complexTypes.nested}
     * sub-category ({@code label} as a text field, {@code count} as a number
     * field) rather than treating the field as an OBJECT placeholder.
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
