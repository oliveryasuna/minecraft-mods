package com.oliveryasuna.mc.coal.neoforge;

import com.oliveryasuna.mc.coal.api.Coal;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Client-side NeoForge entry point.
 * <p>
 * When the COAL provider on the classpath is {@code coal-noop} — the
 * deep-noop last-resort provider bundled with the {@code coal} mod — configs
 * from mods depending on COAL do not persist. Two user-facing nudges surface
 * that:
 * <ul>
 *     <li>A toast shown once when the player first reaches the title screen.
 *         Catches players before they start playing.
 *     <li>A chat message on every world/server join. Catches players who dive
 *         straight into a save.
 * </ul>
 * <p>
 * Registered as a dist-scoped {@link Mod} so it only loads on physical
 * clients.
 */
@Mod(value = "coal", dist = Dist.CLIENT)
public final class CoalNeoForgeClientMod {

    //==================================================
    // Fields
    //==================================================

    private boolean toastShown;

    //==================================================
    // Constructors
    //==================================================

    public CoalNeoForgeClientMod(
            final IEventBus modEventBus,
            final ModContainer container
    ) {
        super();

        // Toast — title-screen, once per game session.
        NeoForge.EVENT_BUS.addListener((final ClientTickEvent.Post event) ->
                maybeShowTitleScreenToast());

        // Chat — world/server join, every time.
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

    //==================================================
    // Methods
    //==================================================

    private void maybeShowTitleScreenToast() {
        if(this.toastShown) return;

        final Minecraft mc = Minecraft.getInstance();
        if(!(mc.screen instanceof TitleScreen)) return;
        if(!Coal.isNoopProvider()) {
            this.toastShown = true;
            return;
        }

        mc.getToastManager().addToast(SystemToast.multiline(
                mc,
                SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                Component.literal("COAL: no backend installed"),
                Component.literal("Configs will not persist. Install a provider mod (e.g. Rubic).")
        ));
        this.toastShown = true;
    }

}
