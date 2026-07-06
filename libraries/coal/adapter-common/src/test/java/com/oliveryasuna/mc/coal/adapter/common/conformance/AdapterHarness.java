package com.oliveryasuna.mc.coal.adapter.common.conformance;

import com.oliveryasuna.mc.coal.adapter.common.AdapterConfigProvider;
import com.oliveryasuna.mc.coal.api.platform.Platform;
import com.oliveryasuna.mc.coal.api.spi.ConfigProvider;
import com.oliveryasuna.mc.coal.api.spi.ConfigProviderFactory;

/**
 * Test-scope {@link ConfigProviderFactory} harness for the conformance suites
 * against the shared {@link AdapterConfigProvider} core.
 * <p>
 * <b>Mirrors — does not import — the real loader-local factories</b>
 * ({@code YaclConfigProviderFactory} in {@code coal-yacl-adapter-fabric} /
 * {@code coal-yacl-adapter-neoforge}, {@code ClothConfigProviderFactory} in
 * the two Cloth adapter modules). Those classes have zero loader-specific
 * dependencies but live in the loader modules for
 * {@code META-INF/services} discovery scope; importing them here would create
 * a cyclic project dependency. The identity triple ({@code name},
 * {@code priority}, {@code coalVersion}) is the same across the loader
 * factories, so this harness is a valid stand-in for conformance behavior
 * that lives entirely in {@link AdapterConfigProvider}.
 * <p>
 * If a loader-local factory ever changes its identity triple, update the
 * corresponding {@code YACL_*} or {@code CLOTH_*} constant below to match.
 */
public final class AdapterHarness {

    //==================================================
    // Static fields
    //==================================================

    public static final String YACL_NAME = "coal-yacl-adapter";
    public static final int YACL_PRIORITY = 100;
    public static final String YACL_COAL_VERSION = "0.1.0";

    public static final String CLOTH_NAME = "coal-cloth-adapter";
    public static final int CLOTH_PRIORITY = 100;
    public static final String CLOTH_COAL_VERSION = "0.1.0";

    //==================================================
    // Static methods
    //==================================================

    public static ConfigProviderFactory yaclFactory() {
        return factory(YACL_NAME, YACL_PRIORITY, YACL_COAL_VERSION);
    }

    public static ConfigProviderFactory clothFactory() {
        return factory(CLOTH_NAME, CLOTH_PRIORITY, CLOTH_COAL_VERSION);
    }

    private static ConfigProviderFactory factory(
            final String name,
            final int priority,
            final String coalVersion
    ) {
        return new ConfigProviderFactory() {

            @Override
            public String name() {
                return name;
            }

            @Override
            public int priority() {
                return priority;
            }

            @Override
            public String coalVersion() {
                return coalVersion;
            }

            @Override
            public ConfigProvider create(final Platform platform) {
                return new AdapterConfigProvider(name, platform);
            }
        };
    }

    //==================================================
    // Constructors
    //==================================================

    private AdapterHarness() {
        super();

        throw new AssertionError();
    }

}
