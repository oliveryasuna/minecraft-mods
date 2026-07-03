package com.oliveryasuna.mc.coal.api.sync;

public record ProtocolVersion(
        int major,
        int minor
) implements Comparable<ProtocolVersion> {

    //==================================================
    // Static methods
    //==================================================

    public static final ProtocolVersion CURRENT = new ProtocolVersion(1, 0);

    //==================================================
    // Methods
    //==================================================

    public boolean isCompatibleWith(final ProtocolVersion other) {
        // For now, we only check for equality.
        return CURRENT.equals(other);
    }

    @Override
    public int compareTo(final ProtocolVersion o) {
        if(this.major != o.major) {
            return Integer.compare(this.major, o.major);
        }

        return Integer.compare(this.minor, o.minor);
    }

}
