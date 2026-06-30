package com.oliveryasuna.mc.omniconfig.testmod;

import com.oliveryasuna.mc.omniconfig.api.annotation.Comment;

/**
 * Plain POJO used as the element type of a {@code List<Waypoint>} field on
 * {@link SampleConfig}. No {@code @Config} annotation — the schema is
 * synthesized at sub-screen open time via
 * {@link com.oliveryasuna.mc.omniconfig.schema.AnnotationSchemaReader#readClass}.
 */
public final class Waypoint {

    //==================================================
    // Fields
    //==================================================

    @Comment("Display name for the waypoint.")
    public String name = "waypoint";

    @Comment("Block X coordinate.")
    public int x;

    @Comment("Block Y coordinate.")
    public int y = 64;

    @Comment("Block Z coordinate.")
    public int z;

    //==================================================
    // Constructors
    //==================================================

    public Waypoint() {
        super();
    }

}
