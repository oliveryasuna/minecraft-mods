package com.oliveryasuna.mc.coal.adapter.common.conformance.cloth;

import com.oliveryasuna.mc.coal.adapter.common.conformance.AdapterHarness;
import com.oliveryasuna.mc.coal.api.spi.ConfigProviderFactory;
import com.oliveryasuna.mc.coal.testkit.AbstractMigrationCapabilityTest;

/**
 * Migration-capability conformance for the Cloth Config adapter identity.
 * <p>
 * {@code AdapterConfigProvider} advertises {@code Capability.MIGRATION}, so
 * per spec §21.2 it MUST extend this class.
 */
public final class ClothMigrationConformanceTest extends AbstractMigrationCapabilityTest {

    //==================================================
    // Constructors
    //==================================================

    public ClothMigrationConformanceTest() {
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
