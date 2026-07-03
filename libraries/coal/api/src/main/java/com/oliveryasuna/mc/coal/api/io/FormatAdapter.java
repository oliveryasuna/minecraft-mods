package com.oliveryasuna.mc.coal.api.io;

import com.oliveryasuna.mc.coal.api.Format;
import com.oliveryasuna.mc.coal.api.schema.Schema;

import java.util.Map;

public interface FormatAdapter {

    //==================================================
    // Methods
    //==================================================

    Format format();

    Map<String, Object> parse(byte[] bytes) throws SerializationException;

    byte[] render(
            Map<String, Object> tree,
            Schema schema
    ) throws SerializationException;

    boolean supportsComments();

}
