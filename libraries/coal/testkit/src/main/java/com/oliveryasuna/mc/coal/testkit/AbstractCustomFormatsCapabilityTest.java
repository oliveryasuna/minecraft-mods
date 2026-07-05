package com.oliveryasuna.mc.coal.testkit;

import com.oliveryasuna.mc.coal.api.Format;
import com.oliveryasuna.mc.coal.api.io.FormatAdapter;
import com.oliveryasuna.mc.coal.api.spi.Capability;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Conformance tests for {@code Capability.CUSTOM_FORMATS} per spec §4.4.
 * Providers advertising {@code Capability.CUSTOM_FORMATS} MUST extend this
 * class and provide a {@link FormatAdapter} for the synthetic format they
 * claim to honor.
 */
public abstract class AbstractCustomFormatsCapabilityTest extends AbstractCoalConformanceTest {

    //==================================================
    // Contract
    //==================================================

    /**
     * The synthetic {@link Format} this provider claims to honor.
     */
    protected abstract Format customFormat();

    /**
     * A fresh {@link FormatAdapter} bound to {@link #customFormat()}.
     */
    protected abstract FormatAdapter newCustomFormatAdapter();

    //==================================================
    // Format subsystem — spec §4
    //==================================================

    @Test
    void formatOfMatchesBuiltInCaseInsensitively() {
        assertSame(Format.TOML, Format.of("toml"), "Format.of('toml') must return the TOML singleton");
        assertSame(Format.TOML, Format.of("Toml"), "case-insensitive match for built-ins (spec §4.3)");
        assertSame(Format.TOML, Format.of("TOML"), "case-insensitive match for built-ins (spec §4.3)");
        assertSame(Format.JSON, Format.of("json"));
        assertSame(Format.JSON5, Format.of("json5"));
    }

    @Test
    void formatOfProducesSyntheticForUnknownId() {
        final Format synthetic = Format.of("hocon");
        assertEquals("hocon", synthetic.id());
        assertEquals("hocon", synthetic.defaultExtension());
        assertNotSame(Format.TOML, synthetic);
    }

    @Test
    void formatOfWithExplicitArgsIgnoresBuiltIns() {
        // Explicit-args overload MUST NOT consult the built-in singletons (spec §4.3).
        final Format synthetic = Format.of("toml", "conf", false);
        assertEquals("toml", synthetic.id());
        assertEquals("conf", synthetic.defaultExtension());
        assertEquals(false, synthetic.supportsComments());
    }

    //==================================================
    // Custom-format adapter
    //==================================================

    @Test
    void customAdapterReportsFormat() {
        final FormatAdapter adapter = newCustomFormatAdapter();
        assertEquals(customFormat(), adapter.format());
    }

    @Test
    void customAdapterRoundtripsScalars() {
        final FormatAdapter adapter = newCustomFormatAdapter();
        final Map<String, Object> tree = new LinkedHashMap<>();
        tree.put("num", 42);
        tree.put("text", "hi");

        final Map<String, Object> parsed = adapter.parse(adapter.render(tree, null));
        assertEquals(42, ((Number)parsed.get("num")).intValue());
        assertEquals("hi", parsed.get("text"));
    }

    @Test
    void providerAdvertisesCustomFormats() {
        assertTrue(this.provider.supports(Capability.CUSTOM_FORMATS),
                "Providers extending AbstractCustomFormatsCapabilityTest must advertise Capability.CUSTOM_FORMATS");
    }

}
