package com.oliveryasuna.mc.omniconfig.io;

import java.io.Serial;

/**
 * Thrown by {@link FormatAdapter} implementations in this module when the
 * underlying engine (NightConfig) cannot parse the input.
 * <p>
 * The {@code core} loader catches this in {@code FileConfigIO.read} and backs
 * the file up.
 * <p>
 * Keeping this in the serialization root package means no NightConfig
 * exception type ever escapes the module boundary.
 */
public final class SerializationException extends RuntimeException {

    //==================================================
    // Static fields
    //==================================================

    @Serial
    private static final long serialVersionUID = 1L;

    //==================================================
    // Constructors
    //==================================================

    public SerializationException(
            final String message,
            final Throwable cause
    ) {
        super(message, cause);
    }

}
