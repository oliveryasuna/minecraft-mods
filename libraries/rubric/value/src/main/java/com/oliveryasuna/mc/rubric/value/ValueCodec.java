package com.oliveryasuna.mc.rubric.value;

/**
 * Converts a Java value of type {@code T} to and from a {@link ValueNode}.
 * <p>
 * {@code decode} throws {@link CodecException} on a shape/type mismatch; the
 * mapper turns that into a reset-to-default correction.
 *
 * @param <T> The type of value to convert.
 */
public interface ValueCodec<T> {

    //==================================================
    // Methods
    //==================================================

    ValueNode encode(T value);

    T decode(ValueNode node);

}
