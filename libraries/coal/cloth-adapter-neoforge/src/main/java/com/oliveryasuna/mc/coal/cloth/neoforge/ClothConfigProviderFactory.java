package com.oliveryasuna.mc.coal.cloth.neoforge;

import com.oliveryasuna.mc.coal.adapter.common.AdapterConfigProvider;
import com.oliveryasuna.mc.coal.api.platform.Platform;
import com.oliveryasuna.mc.coal.api.spi.ConfigProvider;
import com.oliveryasuna.mc.coal.api.spi.ConfigProviderFactory;

/**
 * ServiceLoader-discovered factory for the Cloth Config adapter provider.
 * Priority {@code 100} — ties with the YACL adapter when both are installed;
 * ServiceLoader tie-break picks one. Wraps the shared
 * {@link AdapterConfigProvider} with a Cloth-specific name.
 */
public final class ClothConfigProviderFactory implements ConfigProviderFactory {

    //==================================================
    // Constructors
    //==================================================

    public ClothConfigProviderFactory() {
        super();
    }

    //==================================================
    // ConfigProviderFactory
    //==================================================

    @Override
    public String name() {
        return "coal-cloth-adapter";
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
        return new AdapterConfigProvider("coal-cloth-adapter", platform);
    }

}
