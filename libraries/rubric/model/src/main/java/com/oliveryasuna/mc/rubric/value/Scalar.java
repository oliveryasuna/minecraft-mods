package com.oliveryasuna.mc.rubric.value;

/**
 * A leaf value: {@link String}, {@link Boolean}, a {@link Number}, or
 * {@code null}.
 */
public record Scalar(
        Object value
) implements ValueNode {

}
