package com.oliveryasuna.mc.coal.testkit;

import com.oliveryasuna.mc.coal.api.sync.PayloadCodec;
import com.oliveryasuna.mc.coal.api.sync.ProtocolVersion;
import com.oliveryasuna.mc.coal.api.sync.SyncPayload;
import com.oliveryasuna.mc.coal.api.sync.WireFormatException;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Conformance tests for {@code Capability.SYNC} per spec §16. Providers
 * advertising {@code Capability.SYNC} MUST extend this class and implement
 * {@link #newCodec()} — the codec is what actually crosses the wire, so
 * providers that swap codecs (e.g. binary vs debug-text) can exercise each.
 */
public abstract class AbstractSyncCapabilityTest extends AbstractCoalConformanceTest {

    //==================================================
    // Contract
    //==================================================

    /**
     * Provide a fresh {@link PayloadCodec}. Called once per test.
     */
    protected abstract PayloadCodec newCodec();

    //==================================================
    // ProtocolVersion — spec §16.2
    //==================================================

    @Test
    void protocolVersionCurrentIsOneZero() {
        assertEquals(new ProtocolVersion(1, 0), ProtocolVersion.CURRENT,
                "ProtocolVersion.CURRENT must be (1, 0) at spec revision 0.1.x (spec §16.2)");
    }

    @Test
    void sameMajorIsCompatible() {
        assertTrue(new ProtocolVersion(1, 0).isCompatibleWith(new ProtocolVersion(1, 0)));
        assertTrue(new ProtocolVersion(1, 0).isCompatibleWith(new ProtocolVersion(1, 3)));
        assertTrue(new ProtocolVersion(1, 3).isCompatibleWith(new ProtocolVersion(1, 0)));
    }

    @Test
    void differentMajorIsIncompatible() {
        assertFalse(new ProtocolVersion(1, 0).isCompatibleWith(new ProtocolVersion(2, 0)));
        assertFalse(new ProtocolVersion(2, 0).isCompatibleWith(new ProtocolVersion(1, 5)));
    }

    @Test
    void nullPeerIsIncompatible() {
        assertFalse(ProtocolVersion.CURRENT.isCompatibleWith(null),
                "isCompatibleWith(null) must return false (spec §16.2)");
    }

    @Test
    void compareToOrdersByMajorThenMinor() {
        assertTrue(new ProtocolVersion(1, 0).compareTo(new ProtocolVersion(1, 5)) < 0);
        assertTrue(new ProtocolVersion(2, 0).compareTo(new ProtocolVersion(1, 5)) > 0);
        assertEquals(0, new ProtocolVersion(1, 0).compareTo(new ProtocolVersion(1, 0)));
    }

    //==================================================
    // PayloadCodec — spec §16.4
    //==================================================

    @Test
    void codecRoundtripsHandshake() {
        final PayloadCodec codec = newCodec();
        final SyncPayload original = new SyncPayload.Handshake(ProtocolVersion.CURRENT, Set.of("config-a", "config-b"));
        final SyncPayload roundtripped = codec.decode(codec.encode(original));
        assertEquals(original, roundtripped, "PayloadCodec must roundtrip Handshake (spec §16.4)");
    }

    @Test
    void codecRoundtripsSnapshot() {
        final PayloadCodec codec = newCodec();
        final Map<String, Object> values = new LinkedHashMap<>();
        values.put("num", 42);
        values.put("text", "hi");
        final SyncPayload original = new SyncPayload.Snapshot("config-a", values);
        final SyncPayload roundtripped = codec.decode(codec.encode(original));
        assertEquals(original, roundtripped, "PayloadCodec must roundtrip Snapshot (spec §16.4)");
    }

    @Test
    void codecRoundtripsDelta() {
        final PayloadCodec codec = newCodec();
        final Map<String, Object> changed = new LinkedHashMap<>();
        changed.put("path.a", 1);
        changed.put("path.b", "two");
        final SyncPayload original = new SyncPayload.Delta("config-a", changed);
        final SyncPayload roundtripped = codec.decode(codec.encode(original));
        assertEquals(original, roundtripped, "PayloadCodec must roundtrip Delta (spec §16.4)");
    }

    @Test
    void codecRoundtripsClientEdit() {
        final PayloadCodec codec = newCodec();
        final Map<String, Object> entries = new LinkedHashMap<>();
        entries.put("editable", "new-value");
        final SyncPayload original = new SyncPayload.ClientEdit("config-a", entries);
        final SyncPayload roundtripped = codec.decode(codec.encode(original));
        assertEquals(original, roundtripped, "PayloadCodec must roundtrip ClientEdit (spec §16.4)");
    }

    @Test
    void codecRejectsMalformedBytes() {
        final PayloadCodec codec = newCodec();
        final byte[] garbage = new byte[] {0x00, 0x01, 0x02, 0x03};
        assertThrows(WireFormatException.class, () -> codec.decode(garbage),
                "PayloadCodec.decode must throw WireFormatException on malformed input (spec §16.9)");
    }

    //==================================================
    // Provider capability advertising
    //==================================================

    @Test
    void providerAdvertisesSync() {
        assertTrue(this.provider.supports(com.oliveryasuna.mc.coal.api.spi.Capability.SYNC),
                "Providers extending AbstractSyncCapabilityTest must advertise Capability.SYNC");
    }

}
