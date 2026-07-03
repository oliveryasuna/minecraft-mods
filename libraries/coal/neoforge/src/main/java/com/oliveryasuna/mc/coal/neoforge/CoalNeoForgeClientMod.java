package com.oliveryasuna.mc.coal.neoforge;

import com.oliveryasuna.mc.coal.api.Coal;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Client-side NeoForge entry point.
 * <p>
 * Sends the player a one-time chat message when the COAL provider on the
 * classpath is {@code coal-noop} — the deep-noop last-resort provider bundled
 * with the {@code coal} mod. Without a real provider, configs from mods
 * depending on COAL do not persist. The chat message fires on world/server
 * join so the player sees it at the moment they are most likely to notice
 * config problems.
 * <p>
 * Registered as a dist-scoped {@link Mod} so it only loads on physical
 * clients.
 */
@Mod(value = "coal", dist = Dist.CLIENT)
public final class CoalNeoForgeClientMod {

    //==================================================
    // Constructors
    //==================================================

    public CoalNeoForgeClientMod(
            final IEventBus modEventBus,
            final ModContainer container
    ) {
        super();

        NeoForge.EVENT_BUS.addListener((final ClientPlayerNetworkEvent.LoggingIn event) -> {
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
