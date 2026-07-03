package com.oliveryasuna.mc.rubric.format.toml;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.io.ParsingException;
import com.electronwill.nightconfig.core.io.ParsingMode;
import com.electronwill.nightconfig.core.io.WritingException;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.oliveryasuna.mc.rubric.api.Format;
import com.oliveryasuna.mc.rubric.format.nightconfig.CommentApplier;
import com.oliveryasuna.mc.rubric.format.nightconfig.ValueTreeNightConfigBridge;
import com.oliveryasuna.mc.rubric.io.FormatAdapter;
import com.oliveryasuna.mc.rubric.io.SerializationException;
import com.oliveryasuna.mc.rubric.schema.Schema;
import com.oliveryasuna.mc.rubric.value.ValueTree;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * TOML {@link FormatAdapter} backed by NightConfig.
 * <p>
 * Comments are applied by {@code CommentApplier} when
 * {@link #render(ValueTree, Schema)} is called with a non-null schema.
 */
public final class TomlFormatAdapter implements FormatAdapter {

    //==================================================
    // Constructors
    //==================================================

    public TomlFormatAdapter() {
        super();
    }

    //==================================================
    // Methods
    //==================================================

    @Override
    public Format format() {
        return Format.TOML;
    }

    @Override
    public ValueTree parse(final byte[] bytes) {
        final CommentedConfig cc = CommentedConfig.inMemory();
        try {
            TomlFormat.instance().createParser().parse(new ByteArrayInputStream(bytes), cc, ParsingMode.REPLACE);
        } catch(final ParsingException e) {
            throw new SerializationException("TOML parse failed: " + e.getMessage(), e);
        }

        return ValueTreeNightConfigBridge.fromCommentedConfig(cc);
    }

    @Override
    public byte[] render(
            final ValueTree tree,
            final Schema schema
    ) {
        final CommentedConfig cc = ValueTreeNightConfigBridge.toCommentedConfig(tree);
        if(schema != null) {
            CommentApplier.apply(cc, schema);
        }
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            TomlFormat.instance().createWriter().write(cc, out);
        } catch(final WritingException e) {
            throw new SerializationException("TOML write failed: " + e.getMessage(), e);
        }

        return out.toByteArray();
    }

}
