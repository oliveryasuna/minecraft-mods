package com.oliveryasuna.mc.coal.yacl.testmod;

import com.mojang.blaze3d.platform.InputConstants;
import com.oliveryasuna.mc.coal.api.gui.GuiRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/**
 * Client-side entry for the testmod. Binds {@code K} to open the COAL
 * settings screen for {@link TestmodConfig} via
 * {@link GuiRegistry#open(Minecraft, net.minecraft.client.gui.screens.Screen, com.oliveryasuna.mc.coal.api.config.ConfigManager)}.
 * <p>
 * Press {@code K} in-game (or from the title screen after a world load) to see
 * the YACL adapter's screen render.
 */
public final class TestmodFabricClient implements ClientModInitializer {

    //==================================================
    // Static fields
    //==================================================

    private static final String KEY_CATEGORY = "coal-yacl-testmod";

    //==================================================
    // Constructors
    //==================================================

    public TestmodFabricClient() {
        super();
    }

    //==================================================
    // ClientModInitializer
    //==================================================

    @Override
    public void onInitializeClient() {
        final KeyMapping openKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.coal-yacl-testmod.open",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_K,
                KEY_CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while(openKey.consumeClick()) {
                if(TestmodFabricMain.handle() == null) {
                    return;
                }

                client.setScreen(GuiRegistry.open(
                        client,
                        client.screen,
                        TestmodFabricMain.handle().manager()
                ));
            }
        });
    }

}
