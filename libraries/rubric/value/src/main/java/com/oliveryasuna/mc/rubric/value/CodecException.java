package com.oliveryasuna.mc.rubric.value;

import java.io.Serial;

/**
 * Thrown when a {@link ValueNode} cannot be decoded to the expected Java type.
 */
public final class CodecException extends RuntimeException {

    //==================================================
    // Static fields
    //==================================================

    @Serial
    private static final long serialVersionUID = 1L;

    //==================================================
    // Constructors
    //==================================================

    public CodecException(final String message) {
        super(message);
    }

    public CodecException(
            final String message,
            final Throwable cause
    ) {
        super(message, cause);
    }

}
