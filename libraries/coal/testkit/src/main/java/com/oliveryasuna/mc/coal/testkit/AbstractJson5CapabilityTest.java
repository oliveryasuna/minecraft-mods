package com.oliveryasuna.mc.coal.testkit;

import com.oliveryasuna.mc.coal.api.Format;
import com.oliveryasuna.mc.coal.api.io.FormatAdapter;
import com.oliveryasuna.mc.coal.api.spi.Capability;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Conformance tests for {@code Capability.JSON5} per spec §4. Providers
 * advertising {@code Capability.JSON5} MUST extend this class and implement
 * {@link #newJson5Adapter()}.
 */
public abstract class AbstractJson5CapabilityTest extends AbstractCoalConformanceTest {

    //==================================================
    // Contract
    //==================================================

    /**
     * Provide a fresh {@link FormatAdapter} for {@link Format#JSON5}.
     */
    protected abstract FormatAdapter newJson5Adapter();

    //==================================================
    // Tests
    //==================================================

    @Test
    void adapterReportsJson5Format() {
        final FormatAdapter adapter = newJson5Adapter();
        assertEquals(Format.JSON5, adapter.format(),
                "adapter.format() must equal Format.JSON5");
        assertTrue(adapter.supportsComments(),
                "JSON5 adapter must report supportsComments() == true (spec §4.2)");
    }

    @Test
    void roundtripPreservesScalarsAndTables() {
        final FormatAdapter adapter = newJson5Adapter();
        final Map<String, Object> tree = new LinkedHashMap<>();
        tree.put("num", 42);
        tree.put("text", "hi");
        tree.put("flag", true);

        final Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("child", "value");
        tree.put("group", nested);

        final byte[] rendered = adapter.render(tree, null);
        final Map<String, Object> parsed = adapter.parse(rendered);

        assertEquals(42, ((Number)parsed.get("num")).intValue());
        assertEquals("hi", parsed.get("text"));
        assertEquals(true, parsed.get("flag"));
        assertEquals("value", ((Map<?, ?>)parsed.get("group")).get("child"));
    }

    @Test
    void providerAdvertisesJson5() {
        assertTrue(this.provider.supports(Capability.JSON5),
                "Providers extending AbstractJson5CapabilityTest must advertise Capability.JSON5");
    }

}
