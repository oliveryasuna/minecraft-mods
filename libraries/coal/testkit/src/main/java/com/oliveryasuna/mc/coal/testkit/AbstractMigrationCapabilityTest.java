package com.oliveryasuna.mc.coal.testkit;

import com.oliveryasuna.mc.coal.api.migration.MigrationOp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Conformance tests for {@code Capability.MIGRATION} — the five
 * {@link MigrationOp} factories per spec §9.4. Providers advertising
 * {@code Capability.MIGRATION} MUST extend this class.
 * <p>
 * The tests target {@code MigrationOp.apply(...)} directly against
 * {@code Map<String, Object>} trees rather than exercising a full load-plus-
 * migrate pipeline. Providers that route migrations through a different code
 * path SHOULD add their own integration tests on top.
 */
public abstract class AbstractMigrationCapabilityTest extends AbstractCoalConformanceTest {

    //==================================================
    // Fields
    //==================================================

    protected Map<String, Object> tree;

    //==================================================
    // Setup
    //==================================================

    @BeforeEach
    void seedTree() {
        this.tree = new LinkedHashMap<>();
        this.tree.put("existing", "hello");
        this.tree.put("num", 5);
    }

    //==================================================
    // setValue
    //==================================================

    @Test
    void setValueAlwaysSetsAndCreatesIntermediates() {
        MigrationOp.setValue("new.deep.leaf", "v").apply(this.tree);
        assertEquals("v",
                ((Map<?, ?>)((Map<?, ?>)this.tree.get("new")).get("deep")).get("leaf"),
                "setValue must create intermediate maps on the destination path (spec §9.4)");
    }

    @Test
    void setValueOverwrites() {
        MigrationOp.setValue("existing", "world").apply(this.tree);
        assertEquals("world", this.tree.get("existing"),
                "setValue must overwrite an existing value (spec §9.4)");
    }

    //==================================================
    // setDefault
    //==================================================

    @Test
    void setDefaultOnlyWhenAbsent() {
        MigrationOp.setDefault("existing", "world").apply(this.tree);
        assertEquals("hello", this.tree.get("existing"),
                "setDefault must not overwrite a present value (spec §9.4)");

        MigrationOp.setDefault("added", "world").apply(this.tree);
        assertEquals("world", this.tree.get("added"),
                "setDefault must set an absent key (spec §9.4)");
    }

    @Test
    void setDefaultTreatsPresentNullAsPresent() {
        this.tree.put("nullish", null);
        MigrationOp.setDefault("nullish", "replacement").apply(this.tree);
        assertNull(this.tree.get("nullish"),
                "setDefault must treat a present-but-null value as 'present' (spec §9.4)");
        assertTrue(this.tree.containsKey("nullish"),
                "setDefault must not remove the null entry");
    }

    //==================================================
    // renameKey
    //==================================================

    @Test
    void renameKeyMovesValueAndRemovesSource() {
        MigrationOp.renameKey("existing", "moved").apply(this.tree);
        assertFalse(this.tree.containsKey("existing"), "renameKey must remove the source key (spec §9.4)");
        assertEquals("hello", this.tree.get("moved"), "renameKey must place the value at the destination");
    }

    @Test
    void renameKeyNoOpsWhenSourceAbsent() {
        MigrationOp.renameKey("no-such-key", "renamed").apply(this.tree);
        assertFalse(this.tree.containsKey("renamed"),
                "renameKey with absent source must no-op (spec §9.4)");
    }

    @Test
    void renameKeyThrowsOnDestinationCollision() {
        assertThrows(IllegalStateException.class,
                () -> MigrationOp.renameKey("existing", "num").apply(this.tree),
                "renameKey must throw IllegalStateException when destination exists (spec §9.4)");
    }

    @Test
    void renameKeyCreatesIntermediates() {
        MigrationOp.renameKey("existing", "a.b.c").apply(this.tree);
        assertEquals("hello",
                ((Map<?, ?>)((Map<?, ?>)this.tree.get("a")).get("b")).get("c"),
                "renameKey must create intermediate maps on the destination path (spec §9.4)");
    }

    //==================================================
    // removeKey
    //==================================================

    @Test
    void removeKeyRemovesPresent() {
        MigrationOp.removeKey("existing").apply(this.tree);
        assertFalse(this.tree.containsKey("existing"), "removeKey must remove a present key");
    }

    @Test
    void removeKeyNoOpsOnAbsent() {
        MigrationOp.removeKey("no-such-key").apply(this.tree);
        // No throw — this is the assertion.
    }

    @Test
    void removeKeyDoesNotPruneEmptyParents() {
        MigrationOp.setValue("group.only", "v").apply(this.tree);
        MigrationOp.removeKey("group.only").apply(this.tree);
        assertTrue(this.tree.containsKey("group"),
                "removeKey must not prune an empty parent (spec §9.4)");
        assertTrue(((Map<?, ?>)this.tree.get("group")).isEmpty(),
                "empty parent map should remain");
    }

    //==================================================
    // transform
    //==================================================

    @Test
    void transformAppliesFnToPresent() {
        MigrationOp.transform("num", v -> ((Integer)v) + 1).apply(this.tree);
        assertEquals(6, this.tree.get("num"),
                "transform must replace with fn.apply(current) (spec §9.4)");
    }

    @Test
    void transformNoOpsOnAbsent() {
        MigrationOp.transform("no-such-key", v -> {
            throw new AssertionError("fn must not be invoked for an absent key");
        }).apply(this.tree);
    }

    @Test
    void transformReturningNullSetsNull() {
        MigrationOp.transform("num", v -> null).apply(this.tree);
        assertTrue(this.tree.containsKey("num"),
                "transform returning null must NOT remove the entry (spec §9.4)");
        assertNull(this.tree.get("num"),
                "transform returning null must set the value to null");
    }

}
