package com.oliveryasuna.mc.rubric.testmod;

import com.oliveryasuna.mc.rubric.core.ConfigManager;
import com.oliveryasuna.mc.rubric.fabric.FabricSyncBootstrap;
import com.oliveryasuna.mc.rubric.fabric.gui.RubricGui;
import com.oliveryasuna.mc.rubric.sync.SyncService;
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
            RubricGui.registerScreen(manager);

            final SyncService clientSync = FabricSyncBootstrap.installClient(TestmodFabricMain.sharedCodecs());
            clientSync.register(manager);
            RubricGui.registerSyncService(manager, clientSync);
        }
    }

}
