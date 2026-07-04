package com.oliveryasuna.mc.coal.testkit;

import com.oliveryasuna.mc.coal.api.Coal;
import com.oliveryasuna.mc.coal.api.config.ConfigHandle;
import com.oliveryasuna.mc.coal.api.config.ConfigSpec;
import com.oliveryasuna.mc.coal.api.migration.MigrationSpec;
import com.oliveryasuna.mc.coal.api.spi.Capability;
import com.oliveryasuna.mc.coal.api.spi.ConfigProvider;
import com.oliveryasuna.mc.coal.api.spi.ConfigProviderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Baseline conformance suite for a real {@link ConfigProvider}. Providers
 * extend this class and implement {@link #newFactory()} — JUnit 5 discovers the
 * inherited {@code @Test} methods automatically.
 * <p>
 * <b>Baseline vs coal-noop.</b> {@code coal-noop} is documented in spec §11.5
 * as a normative reference for degraded semantics — it does <i>not</i> satisfy
 * baseline conformance. Baseline tests assume a provider that persists state
 * across {@code set()}/{@code get()}, exposes a {@code Schema}, and honors the
 * annotation surface. Providers whose {@code supports(...)} returns {@code true}
 * for individual capabilities SHOULD additionally extend the matching
 * {@code AbstractXxxCapabilityTest} class.
 * <p>
 * Every test method resets the {@code Coal} entry point via
 * {@link Coal#bootstrap(ConfigProvider)} against a freshly-built provider, so
 * cross-test contamination doesn't occur.
 */
public abstract class AbstractCoalConformanceTest {

    //==================================================
    // Fields
    //==================================================

    /**
     * The factory produced by {@link #newFactory()}, freshly built each test.
     */
    protected ConfigProviderFactory factory;

    /**
     * The provider produced by {@link #factory}, freshly built each test.
     */
    protected ConfigProvider provider;

    //==================================================
    // Contract
    //==================================================

    /**
     * Provide a fresh {@link ConfigProviderFactory}. Called once per test.
     *
     * @return A non-null factory. Its {@link ConfigProviderFactory#create}
     * MUST succeed against a test-scope {@code Platform} discovered via
     * {@link java.util.ServiceLoader}.
     */
    protected abstract ConfigProviderFactory newFactory();

    //==================================================
    // Setup
    //==================================================

    @BeforeEach
    void installProvider() {
        this.factory = newFactory();
        assertNotNull(this.factory, "newFactory() returned null");

        // Route through Coal.bootstrap(ConfigProvider) so the test exercises
        // the public entry point (per spec §3.7). Uses the factory directly to
        // build the provider so tests remain hermetic — no ServiceLoader-order
        // dependence on which factories the test runner happens to see.
        final ConfigProvider p = this.factory.create(newPlatform());
        this.provider = p;
        Coal.bootstrap(p);
    }

    /**
     * Construct a fresh {@code Platform} for the provider factory. Default
     * discovers via {@link java.util.ServiceLoader} — testkit ships its own
     * fallback in {@code META-INF/services}. Providers with their own test
     * Platform (e.g. a mock server-side executor) MAY override.
     */
    protected com.oliveryasuna.mc.coal.api.platform.Platform newPlatform() {
        final java.util.Iterator<com.oliveryasuna.mc.coal.api.platform.Platform> it =
                java.util.ServiceLoader.load(com.oliveryasuna.mc.coal.api.platform.Platform.class).iterator();
        if(!it.hasNext()) {
            throw new IllegalStateException(
                    "No Platform on the test classpath. Provider tests must ship a Platform via META-INF/services, "
                    + "or override newPlatform()."
            );
        }
        return it.next();
    }

    //==================================================
    // Baseline tests
    //==================================================

    // Factory + provider identity
    //--------------------------------------------------

    @Test
    void factoryReportsNonBlankName() {
        final String name = this.factory.name();
        assertNotNull(name, "factory.name() must not be null");
        assertTrue(!name.isBlank(), "factory.name() must not be blank");
    }

    @Test
    void factoryReportsNonNegativePriority() {
        assertTrue(this.factory.priority() >= 0, "priority() must be non-negative");
    }

    @Test
    void factoryReportsCoalVersion() {
        final String v = this.factory.coalVersion();
        assertNotNull(v, "coalVersion() must not be null");
        assertTrue(v.matches("\\d+\\.\\d+\\.\\d+.*"), "coalVersion() must start with MAJOR.MINOR.PATCH — got '" + v + "'");
    }

    @Test
    void providerNameEqualsFactoryName() {
        assertEquals(this.factory.name(), this.provider.name(),
                "ConfigProvider.name() must equal ConfigProviderFactory.name() (spec §11.2)");
    }

    @Test
    void providerReturnsSamePlatform() {
        assertSame(this.provider.platform(), this.provider.platform(),
                "platform() must return a stable reference");
    }

    // Coal.bootstrap wiring
    //--------------------------------------------------

    @Test
    void coalIsBootstrappedAfterExplicitBootstrap() {
        assertTrue(Coal.isBootstrapped(), "Coal.isBootstrapped() must be true after Coal.bootstrap(provider)");
    }

    @Test
    void coalGetProviderReturnsInstalled() {
        assertSame(this.provider, Coal.getProvider(), "Coal.getProvider() must return the explicitly-installed provider");
    }

    @Test
    void coalProviderNameMatches() {
        assertEquals(this.provider.name(), Coal.providerName(),
                "Coal.providerName() must equal provider.name() after bootstrap");
    }

    // Capability declarations
    //--------------------------------------------------

    @Test
    void supportsCapabilityIsStable() {
        for(final Capability cap : Capability.values()) {
            final boolean a = this.provider.supports(cap);
            final boolean b = this.provider.supports(cap);
            assertEquals(a, b, "supports(" + cap + ") returned different values across calls — must be stable (spec §11.3)");
        }
    }

    // Registration surface
    //--------------------------------------------------

    @Test
    void registerAnnotatedConfigReturnsNonNullHandle() {
        final ConfigHandle<BaselineConfig> handle = this.provider.register(BaselineConfig.class, MigrationSpec.empty());
        assertNotNull(handle, "register(Class) must not return null");
        assertNotNull(handle.get(), "handle.get() must not return null");
    }

    @Test
    void registerAnnotatedConfigPopulatesDefaults() {
        final ConfigHandle<BaselineConfig> handle = this.provider.register(BaselineConfig.class, MigrationSpec.empty());
        final BaselineConfig cfg = handle.get();
        assertEquals(42, cfg.intValue, "defaults must survive registration");
        assertEquals("hi", cfg.stringValue, "defaults must survive registration");
        assertTrue(cfg.booleanValue, "defaults must survive registration");
    }

    @Test
    void registerConfigSpecReturnsNonNullHandle() {
        final ConfigSpec spec = new ConfigSpec.Builder("baseline-spec")
                .name("baseline-spec")
                .entry("num", Integer.class, 7)
                .entry("text", String.class, "seven")
                .build();
        final ConfigHandle<?> handle = this.provider.register(spec, MigrationSpec.empty());
        assertNotNull(handle, "register(ConfigSpec) must not return null");
        assertNotNull(handle.get(), "handle.get() must not return null");
    }

    @Test
    void registerRejectsUnannotatedType() {
        assertThrows(IllegalArgumentException.class,
                () -> this.provider.register(NotAConfig.class, MigrationSpec.empty()),
                "register(Class) must throw IllegalArgumentException for a class missing @Config (spec §5.3)");
    }

    @Test
    void duplicateIdRejected() {
        this.provider.register(BaselineConfig.class, MigrationSpec.empty());
        assertThrows(IllegalArgumentException.class,
                () -> this.provider.register(BaselineConfig.class, MigrationSpec.empty()),
                "Registering the same config id twice must throw IllegalArgumentException (spec §11.2)");
    }

    @Test
    void registeredConfigIdsReflectsRegistrations() {
        assertTrue(this.provider.registeredConfigIds().isEmpty(),
                "registeredConfigIds() must start empty");
        this.provider.register(BaselineConfig.class, MigrationSpec.empty());
        assertTrue(this.provider.registeredConfigIds().contains("baseline-testkit"),
                "registeredConfigIds() must include the just-registered id");
    }

    @Test
    void getByIdReturnsRegisteredHandle() {
        final ConfigHandle<BaselineConfig> handle = this.provider.register(BaselineConfig.class, MigrationSpec.empty());
        assertTrue(this.provider.getById("baseline-testkit").isPresent(),
                "getById() must find a just-registered config");
        assertSame(handle, this.provider.getById("baseline-testkit").orElseThrow(),
                "getById() must return the same handle instance");
    }

    @Test
    void getByIdReturnsEmptyForUnknown() {
        assertTrue(this.provider.getById("no-such-config").isEmpty(),
                "getById() must return empty for unknown id");
    }

    // Handle contract
    //--------------------------------------------------

    @Test
    void setUpdatesValueObservably() {
        final ConfigHandle<BaselineConfig> handle = this.provider.register(BaselineConfig.class, MigrationSpec.empty());
        handle.set("intValue", 99);
        assertEquals(99, handle.get().intValue, "set(path, value) must update the value observed via get()");
    }

    @Test
    void snapshotIsNonNull() {
        final ConfigHandle<BaselineConfig> handle = this.provider.register(BaselineConfig.class, MigrationSpec.empty());
        assertNotNull(handle.snapshot(), "snapshot() must not return null");
        assertNotNull(handle.snapshot().capturedAt(), "snapshot.capturedAt() must not return null");
    }

    @Test
    void valueAccessorReturnsCurrentValue() {
        final ConfigHandle<BaselineConfig> handle = this.provider.register(BaselineConfig.class, MigrationSpec.empty());
        handle.set("intValue", 123);
        assertEquals(123, handle.value("intValue", Integer.class).get(),
                "ConfigValue.get() must reflect the current value");
    }

    //==================================================
    // Fixture types
    //==================================================

    @com.oliveryasuna.mc.coal.api.annotation.Config(id = "baseline-testkit", name = "baseline-testkit")
    public static class BaselineConfig {

        public int intValue = 42;
        public String stringValue = "hi";
        public boolean booleanValue = true;

    }

    public static class NotAConfig {

    }

}
