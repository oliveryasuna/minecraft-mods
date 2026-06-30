package com.oliveryasuna.mc.omniconfig.testmod;

import com.oliveryasuna.mc.omniconfig.api.Format;
import com.oliveryasuna.mc.omniconfig.api.annotation.*;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

@Config(id = "omniconfig_testmod", name = "omniconfig_testmod", format = Format.TOML, version = 2)
public final class SampleConfig {

    //==================================================
    // Fields
    //==================================================

    @Comment("Master toggle for the demo feature.")
    public boolean enabled = true;

    @Comment("Tick interval, server-authoritative.")
    @Range(min = 50, max = 5_000)
    @Sync(Sync.Scope.SERVER)
    public int tickIntervalMillis = 200;

    @Comment("Display opacity, client-local.")
    @Range(min = 0.0, max = 1.0)
    public double opacity = 0.8;

    @Comment("Unbounded counter — exercises long field controller.")
    public long bigCounter = 42L;

    @Comment("Quality preset — exercises @OneOf dropdown.")
    @OneOf({"low", "medium", "high"})
    public String quality = "medium";

    @Comment("Smoothing factor — exercises float slider controller.")
    @Range(min = 0.0, max = 2.0)
    public float smoothing = 1.0f;

    @Comment("Highlight tint — exercises @Widget(COLOR) hex picker.")
    @Widget(Widget.Type.COLOR)
    public String tint = "#FF8800";

    @Comment("Icon resource location — exercises MC-type codec (ResourceLocation).")
    public ResourceLocation icon = ResourceLocation.parse("minecraft:textures/gui/icons.png");

    @Comment("Owner UUID — exercises the Mojang Codec<T> bridge (UUIDUtil.STRING_CODEC).")
    public UUID owner = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Comment("Free-form tag list — exercises the scalar-element list editor.")
    public List<String> tags = new ArrayList<>(List.of("alpha", "beta", "gamma"));

    @Comment("Named counters — exercises the Map<String, V> editor.")
    public Map<String, Integer> counters = new LinkedHashMap<>(Map.of(
            "alpha", 1,
            "beta", 2,
            "gamma", 3
    ));

    @Comment("Waypoints — exercises the object-element list editor.")
    public List<Waypoint> waypoints = new ArrayList<>(List.of(
            new Waypoint(),
            new Waypoint()
    ));

    @Comment("Display sub-section — exercises nested-object → CategorySection rendering.")
    public Display display = new Display();

    @Comment("Audio sub-section — second sub-section to exercise multi-section layout.")
    public Audio audio = new Audio();

    //==================================================
    // Constructors
    //==================================================

    public SampleConfig() {
        super();
    }

    //==================================================
    // Inner classes
    //==================================================

    public static final class Display {

        @Comment("Render distance override.")
        @Range(min = 2, max = 32)
        public int renderDistance = 12;

        @Comment("Show the FPS overlay.")
        public boolean showFps = false;

        @Comment("Nested-nested sub-section — exercises recursive CategorySection depth.")
        public Hud hud = new Hud();

        public Display() {
            super();
        }

        public static final class Hud {

            @Comment("Hotbar opacity.")
            @Range(min = 0.0, max = 1.0)
            public double hotbarOpacity = 1.0;

            public Hud() {
                super();
            }

        }

    }

    public static final class Audio {

        @Comment("Master volume.")
        @Range(min = 0.0, max = 1.0)
        public double master = 1.0;

        @Comment("Music volume.")
        @Range(min = 0.0, max = 1.0)
        public double music = 0.5;

        public Audio() {
            super();
        }

    }

}
