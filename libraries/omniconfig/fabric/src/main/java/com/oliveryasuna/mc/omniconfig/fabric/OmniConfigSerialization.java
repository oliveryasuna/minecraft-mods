package com.oliveryasuna.mc.omniconfig.fabric;

import com.oliveryasuna.commons.language.exception.UnsupportedInstantiationException;
import com.oliveryasuna.mc.omniconfig.format.toml.TomlFormatAdapter;
import com.oliveryasuna.mc.omniconfig.io.FormatAdapter;
import com.oliveryasuna.mc.omniconfig.io.file.FileConfigIO;
import com.oliveryasuna.mc.omniconifg.api.Format;

import java.util.Map;

/**
 * Public entry-point for the {@code omniconfig-serialization} module.
 * <p>
 * Mod authors and loader adapters call {@link #defaultIO()} to get a TOML
 * {@link FileConfigIO} ready to hand to a {@code ConfigManager}, or
 * {@link #io(Map)} to register additional formats.
 */
public final class OmniConfigSerialization {

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
                Format.TOML, new TomlFormatAdapter()
                // TODO: Format.JSON, new JsonFormatAdapter(),
                // TODO: Format.JSON5, new Json5FormatAdapter()
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

    private OmniConfigSerialization() {
        super();

        throw new UnsupportedInstantiationException();
    }

}
