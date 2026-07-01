package com.oliveryasuna.mc.rubric.value.codec;

import com.oliveryasuna.mc.rubric.value.Scalar;
import com.oliveryasuna.mc.rubric.value.ValueCodec;
import com.oliveryasuna.mc.rubric.value.ValueNode;

import java.util.function.Function;

/**
 * Generic scalar codec.
 * <p>
 * The decoder coerces the raw parsed value (which a format may deliver as
 * {@link Long}/{@link Double}/{@link String}) into the target type.
 *
 * @param <T> The type of value to convert.
 */
public final class ScalarCodec<T> implements ValueCodec<T> {

    //==================================================
    // Fields
    //==================================================

    private final Function<T, Object> encoder;
    private final Function<Object, T> decoder;

    //==================================================
    // Constructors
    //==================================================

    public ScalarCodec(
            final Function<T, Object> encoder,
            final Function<Object, T> decoder
    ) {
        super();

        this.encoder = encoder;
        this.decoder = decoder;
    }

    @Override
    public ValueNode encode(final T value) {
        return new Scalar(encoder.apply(value));
    }

    @Override
    public T decode(final ValueNode node) {
        if(!(node instanceof Scalar(final Object value))) {
            throw new IllegalArgumentException("Expected scalar, got " + node);
        }

        try {
            return decoder.apply(value);
        } catch(final RuntimeException e) {
            throw new IllegalArgumentException("Cannot decode scalar '" + value + "'", e);
        }
    }

}
