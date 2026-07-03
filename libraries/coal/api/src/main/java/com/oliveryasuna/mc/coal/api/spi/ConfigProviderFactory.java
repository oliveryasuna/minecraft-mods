package com.oliveryasuna.mc.coal.api.spi;

import com.oliveryasuna.mc.coal.api.platform.Platform;

/**
 * Discovered via {@code java.util.ServiceLoader<ConfigProviderFactory>}.
 * <p>
 * Multiple on classpath: highest {@link #priority()} wins, WARN naming every
 * candidate.
 */
public interface ConfigProviderFactory {

    //==================================================
    // Methods
    //==================================================

    // E.g., "coal-rubric", "coal-yacl".
    String name();

    int priority();

    /**
     * Reports the {@code coal-api} version this provider was compiled against.
     */
    String coalVersion();

    ConfigProvider create(Platform platform);

}
