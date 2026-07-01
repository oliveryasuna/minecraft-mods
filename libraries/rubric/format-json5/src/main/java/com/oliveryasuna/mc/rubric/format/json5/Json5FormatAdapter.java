package com.oliveryasuna.mc.rubric.format.json5;

import blue.endless.jankson.*;
import blue.endless.jankson.api.SyntaxError;
import com.oliveryasuna.mc.rubric.api.Format;
import com.oliveryasuna.mc.rubric.io.FormatAdapter;
import com.oliveryasuna.mc.rubric.io.SerializationException;
import com.oliveryasuna.mc.rubric.schema.Schema;
import com.oliveryasuna.mc.rubric.value.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * JSON5 {@link FormatAdapter} backed by {@code blue.endless:jankson}.
 * <p>
 * Parses JSON5 (comments, trailing commas, unquoted keys, etc.) and writes
 * JSON5 with comments attached from the {@link Schema} when one is supplied to
 * {@link #render(ValueTree, Schema)}.
 */
public final class Json5FormatAdapter implements FormatAdapter {

    //==================================================
    // Static fields
    //==================================================

    private static final Jankson PARSER = Jankson.builder().build();
    private static final JsonGrammar GRAMMAR = JsonGrammar.JANKSON;

    //==================================================
    // Static methods
    //==================================================

    // Read (JsonObject -> Section)
    //--------------------------------------------------

    private static Section readObject(final JsonObject source) {
        final Section section = new Section();
        for(final Map.Entry<String, JsonElement> entry : source.entrySet()) {
            section.put(entry.getKey(), readElement(entry.getValue()));
        }

        return section;
    }

    private static ValueNode readElement(final JsonElement element) {
        switch(element) {
            case null -> {
                return new Scalar(null);
            }
            case final JsonNull jsonNull -> {
                return new Scalar(null);
            }
            case final JsonObject object -> {
                return readObject(object);
            }
            case final JsonArray array -> {
                final List<ValueNode> items = new ArrayList<>(array.size());
                for(final JsonElement item : array) {
                    items.add(readElement(item));
                }

                return new ListNode(items);
            }
            case final JsonPrimitive primitive -> {
                final Object raw = primitive.getValue();
                if(raw instanceof final Number number) {
                    return new Scalar(canonicalise(number));
                }

                return new Scalar(raw);
            }
            default -> {
            }
        }

        return new Scalar(element.toString());
    }

    // Write (Section -> JsonObject)
    //--------------------------------------------------

    private static JsonObject writeSection(final Section source) {
        final JsonObject object = new JsonObject();
        for(final Map.Entry<String, ValueNode> entry : source.asMap().entrySet()) {
            final ValueNode node = entry.getValue();
            if(node instanceof Scalar(final Object value) && value == null) {
                continue;
            }

            object.put(entry.getKey(), toElement(node));
        }

        return object;
    }

    private static JsonElement toElement(final ValueNode node) {
        if(node instanceof Scalar(final Object value)) {
            return value == null ? JsonNull.INSTANCE : new JsonPrimitive(canonicaliseIfNumber(value));
        }

        if(node instanceof ListNode(final List<ValueNode> items)) {
            final JsonArray array = new JsonArray();
            for(final ValueNode item : items) {
                array.add(toElement(item));
            }
            return array;
        }

        if(node instanceof final Section section) {
            return writeSection(section);
        }

        throw new IllegalStateException("Unknown ValueNode subtype: " + node);
    }

    // Numeric canonicalization
    //--------------------------------------------------

    private static Object canonicaliseIfNumber(final Object value) {
        return value instanceof final Number n ? canonicalise(n) : value;
    }

    private static Object canonicalise(final Number n) {
        if(n instanceof Byte || n instanceof Short || n instanceof Integer || n instanceof Long) {
            return n.longValue();
        } else if(n instanceof final BigInteger bi) {
            return bi.longValueExact();
        } else if(n instanceof Float || n instanceof Double) {
            return n.doubleValue();
        } else if(n instanceof final BigDecimal bd) {
            return bd.doubleValue();
        }

        return n;
    }

    //==================================================
    // Constructors
    //==================================================

    public Json5FormatAdapter() {
        super();
    }

    //==================================================
    // Methods
    //==================================================

    @Override
    public Format format() {
        return Format.JSON5;
    }

    @Override
    public ValueTree parse(final byte[] bytes) {
        try {
            final JsonObject root = PARSER.load(new ByteArrayInputStream(bytes));
            return new ValueTree(readObject(root));
        } catch(final SyntaxError | IOException e) {
            throw new SerializationException("JSON5 parse failed: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] render(
            final ValueTree tree,
            final Schema schema
    ) {
        final JsonObject root = writeSection(tree.root());
        if(schema != null) {
            Json5CommentApplier.apply(root, schema);
        }

        final StringWriter out = new StringWriter();
        try {
            root.toJson(out, GRAMMAR, 0);
        } catch(final IOException e) {
            throw new SerializationException("JSON5 write failed: " + e.getMessage(), e);
        }

        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

}
