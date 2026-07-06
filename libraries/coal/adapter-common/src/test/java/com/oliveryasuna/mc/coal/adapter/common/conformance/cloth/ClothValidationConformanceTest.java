package com.oliveryasuna.mc.coal.adapter.common.conformance.cloth;

import com.oliveryasuna.mc.coal.adapter.common.conformance.AdapterHarness;
import com.oliveryasuna.mc.coal.api.spi.ConfigProviderFactory;
import com.oliveryasuna.mc.coal.testkit.AbstractValidationCapabilityTest;

/**
 * Validation-capability conformance for the Cloth Config adapter identity.
 * <p>
 * {@code AdapterConfigProvider} advertises {@code Capability.VALIDATION}, so
 * per spec §21.2 it MUST extend this class.
 */
public final class ClothValidationConformanceTest extends AbstractValidationCapabilityTest {

    //==================================================
    // Constructors
    //==================================================

    public ClothValidationConformanceTest() {
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
