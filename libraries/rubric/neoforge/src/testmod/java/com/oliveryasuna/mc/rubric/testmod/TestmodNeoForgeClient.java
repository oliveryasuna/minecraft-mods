package com.oliveryasuna.mc.rubric.testmod;

import com.oliveryasuna.mc.rubric.api.ConfigManager;
import com.oliveryasuna.mc.rubric.neoforge.NeoForgeSyncBootstrap;
import com.oliveryasuna.mc.rubric.loader.gui.RubricGui;
import com.oliveryasuna.mc.rubric.sync.SyncService;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = TestmodNeoForgeMain.MOD_ID, dist = Dist.CLIENT)
public final class TestmodNeoForgeClient {

    //==================================================
    // Constructors
    //==================================================

    public TestmodNeoForgeClient(final IEventBus modEventBus, final ModContainer container) {
        super();

        final ConfigManager<SampleConfig> manager = TestmodNeoForgeMain.manager();
        if(manager != null) {
            RubricGui.registerScreen(manager);

            final SyncService clientSync = NeoForgeSyncBootstrap.installClient(TestmodNeoForgeMain.sharedCodecs());
            clientSync.register(manager);
            RubricGui.registerSyncService(manager, clientSync);

            // Wire the mod-list "Config" button to open this mod's SampleConfig.
            container.registerExtensionPoint(
                    IConfigScreenFactory.class,
                    (mc, parent) -> RubricGui.openFor(Minecraft.getInstance(), parent, manager)
            );
        }
    }

}
