package com.oliveryasuna.mc.coal.api.sync;

import java.io.Serial;

public class WireFormatException extends RuntimeException {

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
