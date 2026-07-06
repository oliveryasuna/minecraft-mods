package com.oliveryasuna.mc.coal.adapter.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.oliveryasuna.mc.coal.api.Format;
import com.oliveryasuna.mc.coal.api.io.FormatAdapter;
import com.oliveryasuna.mc.coal.api.io.SerializationException;
import com.oliveryasuna.mc.coal.api.schema.Schema;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@link Format#JSON} adapter using gson. Comments are silently dropped
 * ({@code Format.JSON.supportsComments() == false}).
 */
final class JsonFormatAdapter implements FormatAdapter {

    //==================================================
    // Static fields
    //==================================================

    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {
    }.getType();

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .serializeNulls()
            .create();

    //==================================================
    // Constructors
    //==================================================

    JsonFormatAdapter() {
        super();
    }

    //==================================================
    // FormatAdapter
    //==================================================

    @Override
    public Format format() {
        return Format.JSON;
    }

    @Override
    public boolean supportsComments() {
        return false;
    }

    @Override
    public Map<String, Object> parse(final byte[] bytes) throws SerializationException {
        try {
            final String text = new String(bytes, StandardCharsets.UTF_8);
            if(text.isBlank()) {
                return new LinkedHashMap<>();
            }

            final Map<String, Object> parsed = GSON.fromJson(text, MAP_TYPE);

            return parsed != null ? parsed : new LinkedHashMap<>();
        } catch(final RuntimeException e) {
            throw new SerializationException("failed to parse JSON: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] render(
            final Map<String, Object> tree,
            final Schema schema
    ) throws SerializationException {
        try {
            return GSON.toJson(tree).getBytes(StandardCharsets.UTF_8);
        } catch(final RuntimeException e) {
            throw new SerializationException("failed to render JSON: " + e.getMessage(), e);
        }
    }

}
