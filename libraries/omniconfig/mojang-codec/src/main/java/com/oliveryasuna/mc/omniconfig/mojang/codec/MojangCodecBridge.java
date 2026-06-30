package com.oliveryasuna.mc.omniconfig.mojang.codec;

import com.google.gson.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.oliveryasuna.commons.language.exception.UnsupportedInstantiationException;
import com.oliveryasuna.mc.omniconfig.value.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Adapter that exposes a Mojang {@link Codec Codec&lt;T&gt;} as an OmniConfig
 * {@link ValueCodec ValueCodec&lt;T&gt;}. The bridge round-trips through
 * {@link JsonOps#INSTANCE} so any consumer of Mojang's serialization stack
 * — game-data records, blockstate references, etc. — can be used as a
 * config field without reimplementing the codec by hand.
 * <p>
 * Encoding shape: whatever the Mojang codec produces. Scalars (string,
 * number, boolean) become {@link Scalar}; arrays become {@link ListNode};
 * objects become {@link Section}. Format adapters (TOML / JSON / JSON5)
 * accept all three shapes already — a {@code BlockPos} field, for example,
 * serializes to a sub-table with {@code x}/{@code y}/{@code z} keys.
 * <p>
 * Decoding failure (the codec's {@code DataResult} carries an error) is
 * surfaced as a {@link CodecException} so the standard load-time correction
 * pipeline resets the entry to its default.
 */
public final class MojangCodecBridge {

    //==================================================
    // Static methods
    //==================================================

    /**
     * Wraps a Mojang codec as an OmniConfig codec.
     *
     * @param codec The Mojang codec to wrap.
     * @param <T>   The codec's value type.
     * @return A {@link ValueCodec} that delegates to {@code codec}.
     */
    public static <T> ValueCodec<T> from(final Codec<T> codec) {
        return new BridgedCodec<>(codec);
    }

    private static ValueNode jsonToNode(final JsonElement element) {
        if(element == null || element.isJsonNull()) {
            return new Scalar(null);
        }
        if(element.isJsonPrimitive()) {
            final JsonPrimitive primitive = element.getAsJsonPrimitive();
            if(primitive.isBoolean()) {
                return new Scalar(primitive.getAsBoolean());
            }
            if(primitive.isNumber()) {
                return new Scalar(primitive.getAsNumber());
            }
            return new Scalar(primitive.getAsString());
        }
        if(element.isJsonArray()) {
            final JsonArray array = element.getAsJsonArray();
            final List<ValueNode> items = new ArrayList<>(array.size());
            for(final JsonElement child : array) {
                items.add(jsonToNode(child));
            }
            return new ListNode(items);
        }
        if(element.isJsonObject()) {
            final Section section = new Section();
            for(final Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                section.put(entry.getKey(), jsonToNode(entry.getValue()));
            }
            return section;
        }
        throw new CodecException("Unsupported JSON kind: " + element);
    }

    private static JsonElement nodeToJson(final ValueNode node) {
        if(node instanceof Scalar(final Object inner)) {
            if(inner == null) {
                return JsonNull.INSTANCE;
            }
            if(inner instanceof final Boolean asBool) {
                return new JsonPrimitive(asBool);
            }
            if(inner instanceof final Number asNum) {
                return new JsonPrimitive(asNum);
            }
            if(inner instanceof final Character asChar) {
                return new JsonPrimitive(asChar);
            }
            return new JsonPrimitive(String.valueOf(inner));
        }
        if(node instanceof final ListNode list) {
            final JsonArray array = new JsonArray(list.items().size());
            for(final ValueNode child : list.items()) {
                array.add(nodeToJson(child));
            }
            return array;
        }
        if(node instanceof final Section section) {
            final JsonObject object = new JsonObject();
            for(final Map.Entry<String, ValueNode> entry : section.asMap().entrySet()) {
                object.add(entry.getKey(), nodeToJson(entry.getValue()));
            }
            return object;
        }
        throw new CodecException("Unsupported ValueNode kind: " + node.getClass().getName());
    }

    //==================================================
    // Constructors
    //==================================================

    private MojangCodecBridge() {
        super();

        throw new UnsupportedInstantiationException();
    }

    //==================================================
    // Nested
    //==================================================

    private static final class BridgedCodec<T> implements ValueCodec<T> {

        private final Codec<T> delegate;

        private BridgedCodec(final Codec<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public ValueNode encode(final T value) {
            final JsonElement element = delegate.encodeStart(JsonOps.INSTANCE, value)
                    .getOrThrow(message -> new CodecException("Mojang codec encode failed: " + message));
            return jsonToNode(element);
        }

        @Override
        public T decode(final ValueNode node) {
            final JsonElement element = nodeToJson(node);
            return delegate.parse(JsonOps.INSTANCE, element)
                    .getOrThrow(message -> new CodecException("Mojang codec decode failed: " + message));
        }

    }

}
