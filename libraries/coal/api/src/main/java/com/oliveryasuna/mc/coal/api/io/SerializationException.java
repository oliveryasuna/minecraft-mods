package com.oliveryasuna.mc.coal.api.io;

import java.io.Serial;

public class SerializationException extends RuntimeException {

    //==================================================
    // Static fields
    //==================================================

    @Serial
    private static final long serialVersionUID = 1L;

    //==================================================
    // Constructors
    //==================================================

    public SerializationException(final String message) {
        super(message);
    }

    public SerializationException(
            final String message,
            final Throwable cause
    ) {
        super(message, cause);
    }

}
