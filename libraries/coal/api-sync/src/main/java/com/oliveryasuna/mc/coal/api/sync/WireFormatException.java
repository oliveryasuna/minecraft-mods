package com.oliveryasuna.mc.coal.api.sync;

public class WireFormatException extends RuntimeException {

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
