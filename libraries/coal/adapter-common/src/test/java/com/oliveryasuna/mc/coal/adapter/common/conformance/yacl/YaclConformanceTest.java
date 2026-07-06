package com.oliveryasuna.mc.coal.adapter.common.conformance.yacl;

import com.oliveryasuna.mc.coal.adapter.common.conformance.AdapterHarness;
import com.oliveryasuna.mc.coal.api.spi.ConfigProviderFactory;
import com.oliveryasuna.mc.coal.testkit.AbstractCoalConformanceTest;

/**
 * Baseline conformance suite for the YACL adapter identity
 * ({@code coal-yacl-adapter}, priority 100).
 */
public final class YaclConformanceTest extends AbstractCoalConformanceTest {

    //==================================================
    // Constructors
    //==================================================

    public YaclConformanceTest() {
        super();
    }

    //==================================================
    // Methods
    //==================================================

    @Override
    protected ConfigProviderFactory newFactory() {
        return AdapterHarness.yaclFactory();
    }

}
