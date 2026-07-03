package com.oliveryasuna.mc.coal.fabric;

import com.oliveryasuna.mc.coal.api.Coal;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Client-side Fabric entry point.
 * <p>
 * Sends the player a one-time chat message when the COAL provider on the
 * classpath is {@code coal-noop} — the deep-noop last-resort provider bundled
 * with the {@code coal} mod. Without a real provider, configs from mods
 * depending on COAL do not persist. The chat message fires on world/server
 * join so the player sees it at the moment they are most likely to notice
 * config problems.
 */
public final class CoalFabricClientMod implements ClientModInitializer {

    //==================================================
    // Constructors
    //==================================================

    public CoalFabricClientMod() {
        super();
    }

    //==================================================
    // Methods
    //==================================================

    @Override
    public void onInitializeClient() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if(!Coal.isNoopProvider()) return;

            final Minecraft mc = Minecraft.getInstance();
            if(mc.player == null) return;

            mc.player.displayClientMessage(
                    Component.literal("[COAL] ").withStyle(ChatFormatting.GOLD)
                            .append(Component.literal(
                                    "No config backend is installed — configs from mods depending on COAL will not persist. "
                                    + "Install a COAL provider mod (e.g. Rubic) alongside COAL."
                            ).withStyle(ChatFormatting.YELLOW)),
                    false
            );
        });
    }

}
