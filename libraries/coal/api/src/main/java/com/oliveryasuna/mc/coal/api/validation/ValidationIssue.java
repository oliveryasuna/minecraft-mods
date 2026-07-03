package com.oliveryasuna.mc.coal.api.validation;

import java.util.Optional;

public record ValidationIssue(
        String message,
        Optional<Object> suggestion
) {

}
