package com.oliveryasuna.mc.omniconfig.sync.protocol;

import java.io.Serial;

/**
 * Thrown when a sync payload cannot be decoded because the bytes do not match
 * the expected wire format (truncated, unknown tag, length out of range, etc.).
 * <p>
 * Surfaced by {@link PayloadCodec} and {@link ValueTreeWireCodec}. {@code sync}
 * callers translate this into an inbound-validation rejection — the
 * conn/payload is dropped rather than partially applied.
 */
public final class WireFormatException extends Exception {

    //==================================================
    // Static fields
    //==================================================

    @Serial
    private static final long serialVersionUID = 1L;

    //==================================================
    // Constructors
    //==================================================

    public WireFormatException(final String message) {
        super(message);
    }

    public WireFormatException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
