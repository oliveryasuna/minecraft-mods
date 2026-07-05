package com.oliveryasuna.mc.coal.yacl.fabric;

import net.fabricmc.api.ModInitializer;

/**
 * Fabric entry point. No work at mod-init — the {@code coal} mod's own
 * {@code onInitialize} calls {@code Coal.bootstrap()}, and our
 * {@link YaclConfigProviderFactory} is picked up via
 * {@code META-INF/services}. This class exists so Fabric has an entrypoint to
 * name in {@code fabric.mod.json}.
 */
public final class YaclAdapterFabricMod implements ModInitializer {

    //==================================================
    // Constructors
    //==================================================

    public YaclAdapterFabricMod() {
        super();
    }

    //==================================================
    // Methods
    //==================================================

    @Override
    public void onInitialize() {
        // Intentionally empty.
    }

}
