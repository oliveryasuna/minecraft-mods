package com.oliveryasuna.mc.coal.api.spi;

import java.io.Serial;

public class ProviderNotFoundException extends RuntimeException {

    //==================================================
    // Static fields
    //==================================================

    @Serial
    private static final long serialVersionUID = 1L;

    //==================================================
    // Constructors
    //==================================================

    public ProviderNotFoundException(final String message) {
        super(message);
    }

}
