package com.oliveryasuna.mc.coal.yacl.fabric;

import com.oliveryasuna.mc.coal.adapter.common.AdapterConfigProvider;
import com.oliveryasuna.mc.coal.api.platform.Platform;
import com.oliveryasuna.mc.coal.api.spi.ConfigProvider;
import com.oliveryasuna.mc.coal.api.spi.ConfigProviderFactory;

/**
 * ServiceLoader-discovered factory for the YACL adapter provider. Priority
 * {@code 100} — beats {@code coal-noop} (priority 0), loses to any provider
 * with a higher declared priority. Wraps the shared
 * {@link AdapterConfigProvider} with a YACL-specific name.
 */
public final class YaclConfigProviderFactory implements ConfigProviderFactory {

    //==================================================
    // Constructors
    //==================================================

    public YaclConfigProviderFactory() {
        super();
    }

    //==================================================
    // ConfigProviderFactory
    //==================================================

    @Override
    public String name() {
        return "coal-yacl-adapter";
    }

    @Override
    public int priority() {
        return 100;
    }

    @Override
    public String coalVersion() {
        return "0.1.0";
    }

    @Override
    public ConfigProvider create(final Platform platform) {
        return new AdapterConfigProvider("coal-yacl-adapter", platform);
    }

}
