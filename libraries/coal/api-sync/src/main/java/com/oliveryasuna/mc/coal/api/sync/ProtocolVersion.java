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

    /**
     * Two {@link ProtocolVersion}s are compatible when their {@link #major()}
     * matches. Minor-version diffs are treated as backward-compatible additive
     * changes — a peer at {@code 1.3} can talk to a peer at {@code 1.0} as long
     * as neither uses a feature the other side doesn't know about (that
     * "know your peer's minor" concern is the payload codec's responsibility,
     * not the handshake's).
     * <p>
     * The comparison is against {@code this} — not {@link #CURRENT} — so
     * providers can hold the local protocol version somewhere other than
     * {@code CURRENT} if needed (e.g. tests, forks).
     */
    public boolean isCompatibleWith(final ProtocolVersion other) {
        if(other == null) {
            return false;
        }

        return this.major == other.major;
    }

    @Override
    public int compareTo(final ProtocolVersion o) {
        if(this.major != o.major) {
            return Integer.compare(this.major, o.major);
        }

        return Integer.compare(this.minor, o.minor);
    }

}
