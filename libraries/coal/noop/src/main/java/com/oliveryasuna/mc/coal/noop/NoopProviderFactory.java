package com.oliveryasuna.mc.coal.noop;

import com.oliveryasuna.mc.coal.api.Coal;
import com.oliveryasuna.mc.coal.api.platform.Platform;
import com.oliveryasuna.mc.coal.api.spi.ConfigProvider;
import com.oliveryasuna.mc.coal.api.spi.ConfigProviderFactory;

/**
 * The no-op provider factory. Discovered via {@code ServiceLoader}.
 * <p>
 * Priority 0 — lowest — so any real provider on the classpath wins. If
 * {@code coal-noop} is the only provider present,
 * {@link com.oliveryasuna.mc.coal.api.Coal#bootstrap()} installs it and every
 * {@code Coal} call becomes a no-op that returns fresh-default POJOs.
 */
public final class NoopProviderFactory implements ConfigProviderFactory {

    //==================================================
    // Constructors
    //==================================================

    public NoopProviderFactory() {
        super();
    }

    //==================================================
    // Methods
    //==================================================

    @Override
    public String name() {
        return Coal.NOOP_PROVIDER_NAME;
    }

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public String coalVersion() {
        return "0.1.0";
    }

    @Override
    public ConfigProvider create(final Platform platform) {
        return new NoopProvider(platform);
    }

}
