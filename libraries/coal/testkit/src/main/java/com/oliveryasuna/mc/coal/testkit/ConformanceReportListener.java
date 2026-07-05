package com.oliveryasuna.mc.coal.testkit;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

/**
 * JUnit Platform {@link TestExecutionListener} that maps testkit abstract-class
 * results onto {@link ConformanceReport} categories and emits JSON per spec
 * §21.3.
 * <p>
 * Providers wire this listener via
 * {@code META-INF/services/org.junit.platform.launcher.TestExecutionListener}
 * in their test resources. Output path is controlled by the
 * {@code coal.testkit.report} system property; when unset, the report is
 * logged only.
 * <p>
 * <b>Category detection.</b> The listener recognizes tests whose enclosing
 * class extends one of the {@code Abstract*} testkit classes by walking the
 * source's class hierarchy. Tests defined outside the testkit's hierarchy are
 * ignored.
 */
public final class ConformanceReportListener implements TestExecutionListener {

    //==================================================
    // Static fields
    //==================================================

    private static final Logger LOGGER = LoggerFactory.getLogger("coal.testkit");

    private static final String TESTKIT_VERSION = "0.1.0";

    private static final String REPORT_PATH_PROPERTY = "coal.testkit.report";
    private static final String PROVIDER_PROPERTY = "coal.testkit.provider";

    //==================================================
    // Fields
    //==================================================

    private final Map<Category, Aggregate> aggregates = new EnumMap<>(Category.class);

    //==================================================
    // Constructors
    //==================================================

    public ConformanceReportListener() {
        super();

        for(final Category c : Category.values()) {
            this.aggregates.put(c, new Aggregate());
        }
    }

    //==================================================
    // Listener callbacks
    //==================================================

    @Override
    public void testPlanExecutionStarted(final TestPlan testPlan) {
        for(final Aggregate a : this.aggregates.values()) {
            a.reset();
        }
    }

    @Override
    public void executionFinished(final TestIdentifier testIdentifier, final TestExecutionResult result) {
        if(!testIdentifier.isTest()) return;

        final Category category = classify(testIdentifier);
        if(category == null) return;

        final Aggregate a = this.aggregates.get(category);
        switch(result.getStatus()) {
            case SUCCESSFUL -> a.passed++;
            case FAILED, ABORTED -> a.failed++;
        }
    }

    @Override
    public void executionSkipped(final TestIdentifier testIdentifier, final String reason) {
        if(!testIdentifier.isTest()) return;
        final Category category = classify(testIdentifier);
        if(category == null) return;
        this.aggregates.get(category).skipped++;
    }

    @Override
    public void testPlanExecutionFinished(final TestPlan testPlan) {
        final String providerName = System.getProperty(PROVIDER_PROPERTY, "unknown");
        final ConformanceReport.Builder builder = ConformanceReport.builder(TESTKIT_VERSION, providerName);

        for(final Map.Entry<Category, Aggregate> entry : this.aggregates.entrySet()) {
            builder.result(entry.getKey().name(), entry.getValue().toResult());
        }

        final ConformanceReport report = builder.build();
        final String json = report.toJson();
        LOGGER.info("COAL conformance report:\n{}", json);

        final String path = System.getProperty(REPORT_PATH_PROPERTY);
        if(path != null && !path.isBlank()) {
            try {
                Files.writeString(Path.of(path), json);
                LOGGER.info("Wrote conformance report to {}", path);
            } catch(final Exception e) {
                LOGGER.warn("Failed to write conformance report to {}: {}", path, e.getMessage());
            }
        }
    }

    //==================================================
    // Helpers
    //==================================================

    private static Category classify(final TestIdentifier id) {
        final Class<?> owningClass = owningClassOf(id);
        if(owningClass == null) return null;

        Class<?> c = owningClass;
        while(c != null && c != Object.class) {
            switch(c.getSimpleName()) {
                case "AbstractCoalConformanceTest" -> {
                    // Baseline only when the extending class is EXACTLY the baseline abstract —
                    // capability subclasses inherit and would double-count. Detect by testing
                    // whether the extending chain contains a capability abstract higher up.
                    if(isBaseline(owningClass)) return Category.BASELINE;
                    return null;
                }
                case "AbstractMigrationCapabilityTest" -> {
                    return Category.MIGRATION;
                }
                case "AbstractSyncCapabilityTest" -> {
                    return Category.SYNC;
                }
                case "AbstractValidationCapabilityTest" -> {
                    return Category.VALIDATION;
                }
                case "AbstractFileWatchCapabilityTest" -> {
                    return Category.FILE_WATCH;
                }
                case "AbstractJson5CapabilityTest" -> {
                    return Category.JSON5;
                }
                case "AbstractCustomFormatsCapabilityTest" -> {
                    return Category.CUSTOM_FORMATS;
                }
                default -> { /* fall through */ }
            }
            c = c.getSuperclass();
        }
        return null;
    }

    private static Class<?> owningClassOf(final TestIdentifier id) {
        return id.getSource().map(src -> {
            if(src instanceof final MethodSource ms) return safeClass(ms.getClassName());
            if(src instanceof final ClassSource cs) return safeClass(cs.getClassName());
            return null;
        }).orElse(null);
    }

    private static Class<?> safeClass(final String name) {
        try {
            return Class.forName(name);
        } catch(final ClassNotFoundException e) {
            return null;
        }
    }

    /**
     * Return {@code true} iff {@code cls}'s superclass chain reaches
     * {@code AbstractCoalConformanceTest} without passing through any capability
     * abstract (which would indicate the test belongs to that capability's
     * category, not baseline).
     */
    private static boolean isBaseline(final Class<?> cls) {
        Class<?> c = cls.getSuperclass();
        while(c != null && c != Object.class) {
            switch(c.getSimpleName()) {
                case "AbstractMigrationCapabilityTest",
                     "AbstractSyncCapabilityTest",
                     "AbstractValidationCapabilityTest",
                     "AbstractFileWatchCapabilityTest",
                     "AbstractJson5CapabilityTest",
                     "AbstractCustomFormatsCapabilityTest" -> {
                    return false;
                }
                case "AbstractCoalConformanceTest" -> {
                    return true;
                }
                default -> { /* fall through */ }
            }
            c = c.getSuperclass();
        }
        return false;
    }

    //==================================================
    // Aggregation
    //==================================================

    private enum Category {
        BASELINE, MIGRATION, SYNC, VALIDATION, FILE_WATCH, JSON5, CUSTOM_FORMATS
    }

    private static final class Aggregate {

        int passed;
        int failed;
        int skipped;

        void reset() {
            this.passed = 0;
            this.failed = 0;
            this.skipped = 0;
        }

        ConformanceReport.Result toResult() {
            if(this.passed == 0 && this.failed == 0 && this.skipped == 0) {
                return ConformanceReport.Result.NOT_ADVERTISED;
            }
            if(this.failed > 0) return ConformanceReport.Result.FAILED;
            if(this.skipped > 0) return ConformanceReport.Result.PARTIAL;
            return ConformanceReport.Result.PASSED;
        }

    }

}
