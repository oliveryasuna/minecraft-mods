package com.oliveryasuna.mc.coal.api;

import com.oliveryasuna.mc.coal.api.annotation.Config;
import com.oliveryasuna.mc.coal.api.config.ConfigHandle;
import com.oliveryasuna.mc.coal.api.migration.MigrationOp;
import com.oliveryasuna.mc.coal.noop.NoopProvider;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

final class SmokeTest {

    //==================================================
    // Constructors
    //==================================================

    private SmokeTest() {
        super();
    }

    //==================================================
    // Tests
    //==================================================

    @Test
    void smokeTest1() {
        final LogCaptor logCaptor = LogCaptor.forName("coal");

        // 1. Bootstrap.
        Coal.bootstrap();
        assertTrue(logCaptor.getLogs().contains("COAL provider 'coal-noop' installed (priority 0)."));
        assertTrue(Coal.isBootstrapped());

        // 2. Register config.
        final ConfigHandle<SmokeConfig1> handle = Coal.register(SmokeConfig1.class);
        assertInstanceOf(NoopProvider.NoopHandle.class, handle);

        // 3. Check defaults.
        final SmokeConfig1 config = handle.get();
        assertInstanceOf(SmokeConfig1.class, config);
        assertTrue(config.booleanValue);
        assertEquals(42, config.intValue);
        assertEquals("Hello, World!", config.stringValue);

        // 4. Check that we can set values.
        assertDoesNotThrow(() -> {
            handle.set("intValue", 100);
        });

        // 5. Check that we can subscribe.
        assertDoesNotThrow(() -> {
            handle.manager().events().subscribe(event -> {
            });
        });

        // 6. Verify `MigrationOp` factories on a `Map<String, Object>` tree.
        final Map<String, Object> tree = new LinkedHashMap<>();
        tree.put("existing", "hello");
        tree.put("num", 5);

        // setValue: always sets, auto-creates intermediates.
        MigrationOp.setValue("new.deep.leaf", "v").apply(tree);
        assertEquals("v", ((Map<?, ?>)((Map<?, ?>)tree.get("new")).get("deep")).get("leaf"));

        // setDefault: only when key is entirely absent.
        MigrationOp.setDefault("existing", "world").apply(tree);
        assertEquals("hello", tree.get("existing"));
        MigrationOp.setDefault("added", "world").apply(tree);
        assertEquals("world", tree.get("added"));

        // renameKey: moves source → destination.
        MigrationOp.renameKey("added", "renamed").apply(tree);
        assertFalse(tree.containsKey("added"));
        assertEquals("world", tree.get("renamed"));

        // renameKey: throws when destination already exists.
        assertThrows(
                IllegalStateException.class,
                () -> {
                    MigrationOp.renameKey("existing", "renamed").apply(tree);
                }
        );

        // renameKey: absent source is a no-op.
        assertDoesNotThrow(() -> {
            MigrationOp.renameKey("nope", "somewhere").apply(tree);
        });

        // removeKey: removes present, no-op on absent.
        MigrationOp.removeKey("existing").apply(tree);
        assertFalse(tree.containsKey("existing"));
        assertDoesNotThrow(() -> {
            MigrationOp.removeKey("nope").apply(tree);
        });

        // transform: applies fn to current, replaces.
        MigrationOp.transform("num", v -> (Integer)v * 2).apply(tree);
        assertEquals(10, tree.get("num"));

        // transform: absent path is no-op — fn is not invoked.
        assertDoesNotThrow(() -> {
            MigrationOp.transform(
                    "missing",
                    v -> {
                        throw new AssertionError("fn should not be invoked on absent path");
                    }
            ).apply(tree);
        });
        assertFalse(tree.containsKey("missing"));

        // 7. Verify `Format.of(...)` case-insensitive built-in singleton lookup.
        assertSame(Format.TOML, Format.of("toml"));
        assertSame(Format.TOML, Format.of("TOML"));
        assertSame(Format.TOML, Format.of("Toml"));
        assertSame(Format.JSON, Format.of("json"));
        assertSame(Format.JSON5, Format.of("JSON5"));

        final Format hocon = Format.of("hocon");
        assertEquals("hocon", hocon.id());
        assertNotSame(Format.TOML, hocon);
        assertNotSame(Format.JSON, hocon);
        // Synthetic `Format`s with the same id are equal (record equality).
        assertEquals(hocon, Format.of("HOCON"));
    }

    //==================================================
    // Nested
    //==================================================

    @Config(id = "smoke-config-1", name = "SmokeConfig1", format = "toml", version = 1)
    public static final class SmokeConfig1 {

        public boolean booleanValue = true;
        public int intValue = 42;
        public String stringValue = "Hello, World!";

    }

}
