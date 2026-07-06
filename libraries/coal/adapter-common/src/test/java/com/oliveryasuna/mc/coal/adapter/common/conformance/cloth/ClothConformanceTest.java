package com.oliveryasuna.mc.coal.adapter.common.conformance.cloth;

import com.oliveryasuna.mc.coal.adapter.common.conformance.AdapterHarness;
import com.oliveryasuna.mc.coal.api.spi.ConfigProviderFactory;
import com.oliveryasuna.mc.coal.testkit.AbstractCoalConformanceTest;

/**
 * Baseline conformance suite for the Cloth Config adapter identity
 * ({@code coal-cloth-adapter}, priority 100).
 */
public final class ClothConformanceTest extends AbstractCoalConformanceTest {

    //==================================================
    // Constructors
    //==================================================

    public ClothConformanceTest() {
        super();
    }

    //==================================================
    // Methods
    //==================================================

    @Override
    protected ConfigProviderFactory newFactory() {
        return AdapterHarness.clothFactory();
    }

}
