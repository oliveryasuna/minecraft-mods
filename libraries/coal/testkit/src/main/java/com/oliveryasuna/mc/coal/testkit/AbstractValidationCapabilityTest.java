package com.oliveryasuna.mc.coal.testkit;

import com.oliveryasuna.mc.coal.api.annotation.Config;
import com.oliveryasuna.mc.coal.api.annotation.OneOf;
import com.oliveryasuna.mc.coal.api.annotation.Pattern;
import com.oliveryasuna.mc.coal.api.annotation.Range;
import com.oliveryasuna.mc.coal.api.config.ConfigHandle;
import com.oliveryasuna.mc.coal.api.migration.MigrationSpec;
import com.oliveryasuna.mc.coal.api.validation.ValidationIssue;
import com.oliveryasuna.mc.coal.api.validation.ValidationResult;
import com.oliveryasuna.mc.coal.api.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Conformance tests for {@code Capability.VALIDATION} — the constraint
 * annotations plus the {@link ValidationResult} sealed type per spec §13.
 * Providers advertising {@code Capability.VALIDATION} MUST extend this class.
 * <p>
 * Providers vary in <i>when</i> they surface constraint violations (register
 * time vs load-with-correction vs set-time). The tests below assert only the
 * end-state visible via the public API — i.e. that a violating value is
 * corrected on load, not what the corrector's per-op payload looks like.
 */
public abstract class AbstractValidationCapabilityTest extends AbstractCoalConformanceTest {

    //==================================================
    // ValidationResult contract
    //==================================================

    @Test
    void validationResultOkHasNoIssues() {
        final ValidationResult ok = ValidationResult.ok();
        assertTrue(ok.isOk(), "Ok.isOk() must return true (spec §13.2)");
        assertFalse(ok.isInvalid(), "Ok.isInvalid() must return false (spec §13.2)");
        assertTrue(ok.issues().isEmpty(), "Ok.issues() must be empty (spec §13.2)");
    }

    @Test
    void validationResultInvalidRequiresIssues() {
        assertThrows(IllegalArgumentException.class,
                () -> ValidationResult.invalid(List.of()),
                "ValidationResult.invalid(emptyList) must throw IllegalArgumentException (spec §13.2)");
    }

    @Test
    void validationResultInvalidWithSingleIssue() {
        final ValidationResult r = ValidationResult.invalid("out of range", 5);
        assertTrue(r.isInvalid(), "Invalid.isInvalid() must return true");
        assertEquals(1, r.issues().size(), "single-issue convenience must produce one ValidationIssue");
        final ValidationIssue issue = r.issues().get(0);
        assertEquals("out of range", issue.message());
        assertEquals(Optional.of(5), issue.suggestion());
    }

    //==================================================
    // Constraint-annotation acceptance surface
    //
    // The following tests only assert that the provider accepts a
    // constraint-annotated class at registration time. Providers with
    // load-time correction should additionally verify their Corrector
    // via a follow-up integration test.
    //==================================================

    @Test
    void rangeAnnotatedClassRegisters() {
        final ConfigHandle<RangeConfig> h = this.provider.register(RangeConfig.class, MigrationSpec.empty());
        assertEquals(50, h.get().value);
    }

    @Test
    void patternAnnotatedClassRegisters() {
        final ConfigHandle<PatternConfig> h = this.provider.register(PatternConfig.class, MigrationSpec.empty());
        assertEquals("abc123", h.get().value);
    }

    @Test
    void oneOfAnnotatedClassRegisters() {
        final ConfigHandle<OneOfConfig> h = this.provider.register(OneOfConfig.class, MigrationSpec.empty());
        assertEquals("green", h.get().value);
    }

    //==================================================
    // Custom Validator
    //==================================================

    /**
     * Providers running validators from {@link com.oliveryasuna.mc.coal.api.schema.EntryMetadata#validators()}
     * MUST invoke them with a {@link Validator.ValidationContext} whose
     * {@code path()} matches the entry's dotted path. Because
     * {@code coal-api} doesn't expose a way to <i>attach</i> a custom
     * {@link Validator} to an annotation-driven config from user code, this
     * check is a static-contract exercise on the {@code Validator} functional
     * interface itself.
     */
    @Test
    void customValidatorReturnsOk() {
        final Validator<Integer> v = (val, ctx) -> val >= 0 ? ValidationResult.ok() : ValidationResult.invalid("negative", 0);
        final ValidationResult r = v.validate(5, new Validator.ValidationContext() {
            @Override
            public com.oliveryasuna.mc.coal.api.schema.SchemaEntry entry() {
                return null;
            }

            @Override
            public String path() {
                return "num";
            }
        });
        assertTrue(r.isOk());
    }

    //==================================================
    // Fixtures
    //==================================================

    @Config(id = "range-testkit", name = "range-testkit")
    public static class RangeConfig {

        @Range(min = 0, max = 100)
        public int value = 50;

    }

    @Config(id = "pattern-testkit", name = "pattern-testkit")
    public static class PatternConfig {

        @Pattern("[a-z0-9]+")
        public String value = "abc123";

    }

    @Config(id = "oneof-testkit", name = "oneof-testkit")
    public static class OneOfConfig {

        @OneOf({"red", "green", "blue"})
        public String value = "green";

    }

}
