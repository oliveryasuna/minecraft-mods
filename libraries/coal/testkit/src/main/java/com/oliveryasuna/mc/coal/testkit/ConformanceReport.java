package com.oliveryasuna.mc.coal.testkit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A serializable snapshot of a testkit run per spec §21.3.
 * <p>
 * Producers construct instances via {@link #builder(String, String)} and pass
 * per-capability results in. The final {@link #toJson()} rendering is a stable
 * JSON document intended to accompany a provider's release artifacts.
 *
 * <h2>Wire format</h2>
 * <pre>
 * {
 *   "testkitVersion": "0.1.0",
 *   "provider": "coal-rubric",
 *   "generatedAt": "2026-07-03T12:34:56Z",
 *   "results": {
 *     "BASELINE":      "PASSED",
 *     "MIGRATION":     "PASSED",
 *     "SYNC":          "PARTIAL",
 *     "VALIDATION":    "PASSED",
 *     "FILE_WATCH":    "NOT_ADVERTISED",
 *     "GUI_DELEGATION":"NOT_ADVERTISED",
 *     "JSON5":         "PASSED",
 *     "CUSTOM_FORMATS":"NOT_ADVERTISED"
 *   }
 * }
 * </pre>
 */
public final class ConformanceReport {

    //==================================================
    // Static fields
    //==================================================

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    //==================================================
    // Nested types
    //==================================================

    public enum Result {
        PASSED, PARTIAL, FAILED, NOT_ADVERTISED
    }

    //==================================================
    // Fields
    //==================================================

    private final String testkitVersion;
    private final String provider;
    private final Instant generatedAt;
    private final Map<String, Result> results;

    //==================================================
    // Constructors
    //==================================================

    private ConformanceReport(
            final String testkitVersion,
            final String provider,
            final Instant generatedAt,
            final Map<String, Result> results
    ) {
        super();

        this.testkitVersion = testkitVersion;
        this.provider = provider;
        this.generatedAt = generatedAt;
        this.results = Map.copyOf(results);
    }

    //==================================================
    // Static methods
    //==================================================

    public static Builder builder(final String testkitVersion, final String provider) {
        return new Builder(testkitVersion, provider);
    }

    //==================================================
    // Methods
    //==================================================

    public String testkitVersion() {
        return this.testkitVersion;
    }

    public String provider() {
        return this.provider;
    }

    public Instant generatedAt() {
        return this.generatedAt;
    }

    public Map<String, Result> results() {
        return this.results;
    }

    public String toJson() {
        final Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("testkitVersion", this.testkitVersion);
        doc.put("provider", this.provider);
        doc.put("generatedAt", this.generatedAt.toString());
        doc.put("results", this.results);
        return GSON.toJson(doc);
    }

    //==================================================
    // Builder
    //==================================================

    public static final class Builder {

        private final String testkitVersion;
        private final String provider;
        private final Map<String, Result> results = new LinkedHashMap<>();
        private Instant generatedAt;

        private Builder(final String testkitVersion, final String provider) {
            super();

            this.testkitVersion = testkitVersion;
            this.provider = provider;
        }

        public Builder result(final String category, final Result result) {
            this.results.put(category, result);
            return this;
        }

        public Builder generatedAt(final Instant generatedAt) {
            this.generatedAt = generatedAt;
            return this;
        }

        public ConformanceReport build() {
            return new ConformanceReport(
                    this.testkitVersion,
                    this.provider,
                    this.generatedAt != null ? this.generatedAt : Instant.now(),
                    this.results
            );
        }

    }

}
