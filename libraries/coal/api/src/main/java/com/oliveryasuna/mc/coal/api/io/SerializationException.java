package com.oliveryasuna.mc.coal.api.io;

public class SerializationException extends RuntimeException {

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
