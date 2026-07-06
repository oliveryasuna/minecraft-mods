package com.oliveryasuna.mc.coal.cloth.testmod;

import com.mojang.blaze3d.platform.InputConstants;
import com.oliveryasuna.mc.coal.api.gui.GuiRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Client-side entry for the NG testmod. Binds {@code K} to open the COAL
 * settings screen for {@link TestmodConfig} via
 * {@link GuiRegistry#open(Minecraft, net.minecraft.client.gui.screens.Screen, com.oliveryasuna.mc.coal.api.config.ConfigManager)}.
 */
@Mod(value = "coal_cloth_adapter_testmod", dist = Dist.CLIENT)
public final class TestmodNeoForgeClient {

    //==================================================
    // Static fields
    //==================================================

    private static final String KEY_CATEGORY = "coal-cloth-testmod";

    private static KeyMapping OPEN_KEY;

    //==================================================
    // Constructors
    //==================================================

    public TestmodNeoForgeClient(final IEventBus modEventBus) {
        super();

        modEventBus.addListener(TestmodNeoForgeClient::registerKeys);
        NeoForge.EVENT_BUS.addListener(TestmodNeoForgeClient::onClientTick);
    }

    //==================================================
    // Static methods
    //==================================================

    @SubscribeEvent
    private static void registerKeys(final RegisterKeyMappingsEvent event) {
        OPEN_KEY = new KeyMapping(
                "key.coal-cloth-testmod.open",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_K,
                KEY_CATEGORY
        );
        event.register(OPEN_KEY);
    }

    private static void onClientTick(final ClientTickEvent.Post event) {
        if(OPEN_KEY == null) return;
        final Minecraft client = Minecraft.getInstance();
        while(OPEN_KEY.consumeClick()) {
            if(TestmodNeoForgeMain.handle() == null) return;
            client.setScreen(GuiRegistry.open(
                    client,
                    client.screen,
                    TestmodNeoForgeMain.handle().manager()
            ));
        }
    }

}
