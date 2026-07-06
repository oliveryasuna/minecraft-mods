package com.oliveryasuna.mc.coal.adapter.common.conformance.yacl;

import com.oliveryasuna.mc.coal.adapter.common.conformance.AdapterHarness;
import com.oliveryasuna.mc.coal.api.spi.ConfigProviderFactory;
import com.oliveryasuna.mc.coal.testkit.AbstractValidationCapabilityTest;

/**
 * Validation-capability conformance for the YACL adapter identity.
 * <p>
 * {@code AdapterConfigProvider} advertises {@code Capability.VALIDATION}, so
 * per spec §21.2 it MUST extend this class.
 */
public final class YaclValidationConformanceTest extends AbstractValidationCapabilityTest {

    //==================================================
    // Constructors
    //==================================================

    public YaclValidationConformanceTest() {
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
