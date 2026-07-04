package com.oliveryasuna.mc.coal.yacl.testmod;

import com.oliveryasuna.mc.coal.api.Coal;
import com.oliveryasuna.mc.coal.api.config.ConfigHandle;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry for the testmod. Registers {@link TestmodConfig} against COAL at
 * mod-init.
 */
public final class TestmodFabricMain implements ModInitializer {

    //==================================================
    // Static fields
    //==================================================

    private static final Logger LOGGER = LoggerFactory.getLogger("coal-yacl-testmod");

    private static volatile ConfigHandle<TestmodConfig> HANDLE;

    //==================================================
    // Static methods
    //==================================================

    public static ConfigHandle<TestmodConfig> handle() {
        return HANDLE;
    }

    //==================================================
    // Constructors
    //==================================================

    public TestmodFabricMain() {
        super();
    }

    //==================================================
    // Methods
    //==================================================

    @Override
    public void onInitialize() {
        HANDLE = Coal.register(TestmodConfig.class);
        LOGGER.info("Registered testmod config — installed provider: {}", Coal.providerName());
    }

}
