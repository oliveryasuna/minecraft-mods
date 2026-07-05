package com.oliveryasuna.mc.coal.fabric;

import com.oliveryasuna.mc.coal.api.Coal;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

/**
 * Client-side Fabric entry point.
 * <p>
 * When the COAL provider on the classpath is {@code coal-noop} — the
 * deep-noop last-resort provider bundled with the {@code coal} mod — configs
 * from mods depending on COAL do not persist. Two user-facing nudges surface
 * that:
 * <ul>
 *     <li>
 *         A toast shown once when the player first reaches the title screen.
 *         Catches players before they start playing.
 *     </li>
 *     <li>
 *         A chat message on every world/server join. Catches players who dive
 *         straight into a save.
 *     </li>
 * </ul>
 */
public final class CoalFabricClientMod implements ClientModInitializer {

    //==================================================
    // Fields
    //==================================================

    private boolean toastShown;

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
        // Toast — title-screen, once per game session.
        ClientTickEvents.END_CLIENT_TICK.register(this::maybeShowTitleScreenToast);

        // Chat — world/server join, every time.
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

    private void maybeShowTitleScreenToast(final Minecraft mc) {
        if(this.toastShown) return;
        if(!(mc.screen instanceof TitleScreen)) return;
        if(!Coal.isNoopProvider()) {
            // Not noop — never show, but flip the flag so we stop checking.
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
