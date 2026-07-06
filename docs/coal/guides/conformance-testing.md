---
title: Conformance-test a provider
---

# Conformance-test a provider

`coal-testkit` is a TCK-style JUnit 5 suite. Providers extend abstract test classes and get spec conformance checks for free — no boilerplate beyond a `newFactory()` implementation.

Normative reference: [spec §21](../spec/#21-conformance).

## The three test suites

### Baseline

Every provider extends [`AbstractCoalConformanceTest`](https://github.com/oliveryasuna/minecraft-mods/blob/main/libraries/coal/testkit/src/main/java/com/oliveryasuna/mc/coal/testkit/AbstractCoalConformanceTest.java). Checks the invariants that are **not** capability-conditional:

- Registration returns a live `ConfigHandle`.
- `get()` returns the schema defaults on a fresh registration.
- `set(...)` mutates state.
- `save()` + `load()` round-trip.
- `Schema` exposes the declared entries and categories.
- `EventBus` dispatches `ChangeEvent` on `set(...)`.
- `Origin` bookkeeping is coherent.

Note: `coal-noop` is documented ([spec §11.5](../spec/#115-the-coal-noop-provider)) as a normative reference for **degraded** semantics — it does *not* satisfy baseline. Baseline assumes a provider that actually persists state.

### Per-capability

One abstract class per capability. Providers extend the ones matching their advertised capabilities:

| Test class                              | Extends when `supports(...)` returns `true` for |
|-----------------------------------------|-------------------------------------------------|
| `AbstractMigrationCapabilityTest`       | `MIGRATION`                                     |
| `AbstractValidationCapabilityTest`      | `VALIDATION`                                    |
| `AbstractSyncCapabilityTest`            | `SYNC`                                          |
| `AbstractFileWatchCapabilityTest`       | `FILE_WATCH`                                    |
| `AbstractJson5CapabilityTest`           | `JSON5`                                         |
| `AbstractCustomFormatsCapabilityTest`   | `CUSTOM_FORMATS`                                |

There's no `AbstractGuiDelegationCapabilityTest` — GUI rendering is inherently client-context-dependent and outside the testkit's headless scope.

### The `ConformanceReport`

A serializable snapshot of a testkit run. Providers publish it as JSON with each release.

```json
{
  "testkitVersion": "0.1.0",
  "provider": "coal-mycompany",
  "generatedAt": "2026-03-11T12:34:56Z",
  "results": {
    "BASELINE":       "PASSED",
    "MIGRATION":      "PASSED",
    "VALIDATION":     "PASSED",
    "SYNC":           "NOT_ADVERTISED",
    "FILE_WATCH":     "PARTIAL",
    "GUI_DELEGATION": "NOT_ADVERTISED",
    "JSON5":          "NOT_ADVERTISED",
    "CUSTOM_FORMATS": "NOT_ADVERTISED"
  }
}
```

Result values: `PASSED`, `PARTIAL`, `FAILED`, `NOT_ADVERTISED`. Build via `ConformanceReport.builder(name, version)`.

## Setting up

### Add the testkit as a test dependency

```kotlin
// build.gradle.kts (in your provider project)
dependencies {
    testImplementation("com.oliveryasuna.mc:coal-testkit:<version>")
    testImplementation("com.oliveryasuna.mc:coal-api:<version>")

    // JUnit 5 (Jupiter) if you don't already have it
    testImplementation(platform("org.junit:junit-bom:5.11.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
```

### Ship a `Platform` for tests

The testkit bundles `TestkitPlatform` (registered via `META-INF/services/com.oliveryasuna.mc.coal.api.platform.Platform` inside the testkit jar). It uses a per-JVM temp directory as `configDir()` so tests don't share disk state.

If your provider needs a specific platform (e.g., you want to point `configDir()` at a specific location), override `newPlatform()` in your abstract test subclass rather than replacing the `META-INF/services` entry.

## Writing the baseline test

```java
package com.example.coal.mycompany;

import com.oliveryasuna.mc.coal.api.spi.ConfigProviderFactory;
import com.oliveryasuna.mc.coal.testkit.AbstractCoalConformanceTest;

public final class MyCompanyConformanceTest extends AbstractCoalConformanceTest {

    @Override
    protected ConfigProviderFactory newFactory() {
        return new MyCompanyProviderFactory();
    }
}
```

That's it. JUnit auto-discovers the inherited `@Test` methods. Every test method resets `Coal` via `Coal.bootstrap(ConfigProvider)` against a fresh provider — no cross-test contamination.

## Adding per-capability tests

One class per advertised capability:

```java
public final class MyCompanyMigrationTest extends AbstractMigrationCapabilityTest {
    @Override
    protected ConfigProviderFactory newFactory() { return new MyCompanyProviderFactory(); }
}

public final class MyCompanyValidationTest extends AbstractValidationCapabilityTest {
    @Override
    protected ConfigProviderFactory newFactory() { return new MyCompanyProviderFactory(); }
}
```

Skip the ones you don't advertise. The report shows `NOT_ADVERTISED` for missing capabilities — that's fine and honest, not a failure.

## Producing the `ConformanceReport`

The testkit ships a `ConformanceReportListener` you register as a JUnit listener. It aggregates results across every capability test class in the run and writes JSON when the run completes.

Wire it via `src/test/resources/META-INF/services/org.junit.platform.launcher.TestExecutionListener` with the FQCN of the listener. Or configure programmatically via a test-run script.

Alternately, build the report by hand from your test results — see [`ConformanceReport`](https://github.com/oliveryasuna/minecraft-mods/blob/main/libraries/coal/testkit/src/main/java/com/oliveryasuna/mc/coal/testkit/ConformanceReport.java) for the builder API.

## CI integration

Standard JUnit gradle. Both first-party adapters plug `coal-testkit` into their `check` task; the same works for you.

The [nightly workflow](https://github.com/oliveryasuna/minecraft-mods/blob/main/.github/workflows/nightly.yml) in this repo runs `check` on every leaf module, which picks up conformance tests automatically once you add them. If you're publishing artifacts, upload the generated `conformance-report.json` alongside the jar so users can see coverage.

## Related

- [Write a provider](./writing-a-provider) — build one, then test it.
- [Capabilities](../concepts/capabilities) — what you're advertising.
- [Specification §21](../spec/#21-conformance) — normative rules.
