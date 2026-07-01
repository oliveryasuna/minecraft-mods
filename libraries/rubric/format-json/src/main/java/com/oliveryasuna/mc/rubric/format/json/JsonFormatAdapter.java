package com.oliveryasuna.mc.rubric.format.json;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.io.ParsingException;
import com.electronwill.nightconfig.core.io.ParsingMode;
import com.electronwill.nightconfig.core.io.WritingException;
import com.electronwill.nightconfig.json.JsonFormat;
import com.oliveryasuna.mc.rubric.api.Format;
import com.oliveryasuna.mc.rubric.format.nightconfig.ValueTreeNightConfigBridge;
import com.oliveryasuna.mc.rubric.io.FormatAdapter;
import com.oliveryasuna.mc.rubric.io.SerializationException;
import com.oliveryasuna.mc.rubric.schema.Schema;
import com.oliveryasuna.mc.rubric.value.ValueTree;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Plain JSON {@link FormatAdapter} backed by NightConfig.
 * <p>
 * JSON has no comment syntax — the {@link Schema} argument to
 * {@link #render(ValueTree, Schema)} is accepted for interface parity but its
 * comments are not emitted.
 */
public final class JsonFormatAdapter implements FormatAdapter {

    //==================================================
    // Constructors
    //==================================================

    public JsonFormatAdapter() {
        super();
    }

    //==================================================
    // Methods
    //==================================================

    @Override
    public Format format() {
        return Format.JSON;
    }

    @Override
    public ValueTree parse(final byte[] bytes) {
        final CommentedConfig cc = CommentedConfig.inMemory();
        try {
            JsonFormat.fancyInstance().createParser().parse(new ByteArrayInputStream(bytes), cc, ParsingMode.REPLACE);
        } catch(final ParsingException e) {
            throw new SerializationException("JSON parse failed: " + e.getMessage(), e);
        }

        return ValueTreeNightConfigBridge.fromCommentedConfig(cc);
    }

    @Override
    public byte[] render(
            final ValueTree tree,
            final Schema schema
    ) {
        final CommentedConfig cc = ValueTreeNightConfigBridge.toCommentedConfig(tree);
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            JsonFormat.fancyInstance().createWriter().write(cc, out);
        } catch(final WritingException e) {
            throw new SerializationException("JSON write failed: " + e.getMessage(), e);
        }

        return out.toByteArray();
    }

}
