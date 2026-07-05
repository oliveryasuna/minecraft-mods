package com.oliveryasuna.mc.coal.api.event;

import com.oliveryasuna.mc.coal.api.config.Origin;

import java.time.Instant;

public record ChangeEvent(
        String path,
        Object oldValue,
        Object newValue,
        Origin origin,
        Instant at
) {

}
