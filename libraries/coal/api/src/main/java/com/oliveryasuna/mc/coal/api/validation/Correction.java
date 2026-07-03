package com.oliveryasuna.mc.coal.api.validation;

public record Correction(
        String path,
        Object before,
        Object after,
        String reason
) {

}
