package com.oliveryasuna.mc.rubric.loader;

import com.oliveryasuna.commons.language.exception.UnsupportedInstantiationException;
import com.oliveryasuna.mc.rubric.api.Format;
import com.oliveryasuna.mc.rubric.format.json.JsonFormatAdapter;
import com.oliveryasuna.mc.rubric.format.json5.Json5FormatAdapter;
import com.oliveryasuna.mc.rubric.format.toml.TomlFormatAdapter;
import com.oliveryasuna.mc.rubric.io.FormatAdapter;
import com.oliveryasuna.mc.rubric.io.file.FileConfigIO;

import java.util.Map;

/**
 * Public entry-point for the {@code rubric-serialization} module.
 * <p>
 * Mod authors and loader adapters call {@link #defaultIO()} to get a TOML
 * {@link FileConfigIO} ready to hand to a {@code ConfigManager}, or
 * {@link #io(Map)} to register additional formats.
 */
public final class RubricSerialization {

    //==================================================
    // Static methods
    //==================================================

    /**
     * TOML-only IO. The intended default for most consumers.
     */
    public static FileConfigIO defaultIO() {
        return new FileConfigIO(Map.of(Format.TOML, new TomlFormatAdapter()));
    }

    /**
     * IO with TOML + JSON + JSON5 adapters registered.
     */
    public static FileConfigIO allFormatsIO() {
        return new FileConfigIO(Map.of(
                Format.TOML, new TomlFormatAdapter(),
                Format.JSON, new JsonFormatAdapter(),
                Format.JSON5, new Json5FormatAdapter()
        ));
    }

    /**
     * IO with a custom registry — for consumers who add their own adapters.
     */
    public static FileConfigIO io(final Map<Format, FormatAdapter> adapters) {
        return new FileConfigIO(adapters);
    }

    //==================================================
    // Constructors
    //==================================================

    private RubricSerialization() {
        super();

        throw new UnsupportedInstantiationException();
    }

}
