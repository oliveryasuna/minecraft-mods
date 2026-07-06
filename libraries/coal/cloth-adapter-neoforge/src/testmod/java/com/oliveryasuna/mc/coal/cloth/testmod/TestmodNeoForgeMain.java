package com.oliveryasuna.mc.coal.cloth.testmod;

import com.oliveryasuna.mc.coal.api.Coal;
import com.oliveryasuna.mc.coal.api.config.ConfigHandle;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry for the NG testmod. Registers {@link TestmodConfig} against COAL
 * at mod-init.
 */
@Mod("coal_cloth_adapter_testmod")
public final class TestmodNeoForgeMain {

    //==================================================
    // Static fields
    //==================================================

    private static final Logger LOGGER = LoggerFactory.getLogger("coal-cloth-testmod");

    private static volatile ConfigHandle<TestmodConfig> HANDLE;

    //==================================================
    // Constructors
    //==================================================

    public TestmodNeoForgeMain(final IEventBus modEventBus) {
        super();

        HANDLE = Coal.register(TestmodConfig.class);
        LOGGER.info("Registered testmod config — installed provider: {}", Coal.providerName());
    }

    //==================================================
    // Static methods
    //==================================================

    public static ConfigHandle<TestmodConfig> handle() {
        return HANDLE;
    }

}
