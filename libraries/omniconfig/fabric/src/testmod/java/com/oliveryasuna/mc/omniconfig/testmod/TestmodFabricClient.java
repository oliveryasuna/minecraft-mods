package com.oliveryasuna.mc.omniconfig.testmod;

import com.oliveryasuna.mc.omniconfig.api.ConfigManager;
import com.oliveryasuna.mc.omniconfig.fabric.FabricSyncBootstrap;
import com.oliveryasuna.mc.omniconfig.fabric.gui.OmniConfigGui;
import com.oliveryasuna.mc.omniconfig.sync.SyncService;
import net.fabricmc.api.ClientModInitializer;

public final class TestmodFabricClient implements ClientModInitializer {

    //==================================================
    // Constructors
    //==================================================

    public TestmodFabricClient() {
        super();
    }

    //==================================================
    // Methods
    //==================================================

    @Override
    public void onInitializeClient() {
        final ConfigManager<SampleConfig> manager = TestmodFabricMain.manager();
        if(manager != null) {
            OmniConfigGui.registerScreen(manager);

            final SyncService clientSync = FabricSyncBootstrap.installClient(TestmodFabricMain.sharedCodecs());
            clientSync.register(manager);
            OmniConfigGui.registerSyncService(manager, clientSync);
        }
    }

}
