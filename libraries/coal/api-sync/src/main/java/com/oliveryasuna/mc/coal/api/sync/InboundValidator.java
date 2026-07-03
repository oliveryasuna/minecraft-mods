package com.oliveryasuna.mc.coal.api.sync;

import java.util.Map;

@FunctionalInterface
public interface InboundValidator {

    //==================================================
    // Methods
    //==================================================

    boolean accept(
            String configId,
            Map<String, Object> values
    );

}
