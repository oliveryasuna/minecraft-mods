package com.oliveryasuna.mc.rubric.testmod;

import com.oliveryasuna.mc.rubric.api.annotation.Comment;

/**
 * Plain POJO used as the element type of a {@code List<Waypoint>} field on
 * {@link SampleConfig}.
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
