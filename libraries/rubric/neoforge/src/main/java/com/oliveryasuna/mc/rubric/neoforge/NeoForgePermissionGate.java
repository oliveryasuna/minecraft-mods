package com.oliveryasuna.mc.rubric.neoforge;

import com.oliveryasuna.mc.rubric.platform.PermissionGate;
import net.minecraft.server.level.ServerPlayer;

/**
 * NeoForge implementation of {@link PermissionGate}. Mojmap names.
 */
public final class NeoForgePermissionGate implements PermissionGate {

    //==================================================
    // Methods
    //==================================================

    @Override
    public boolean canEdit(
            final Object actor,
            final int requiredLevel
    ) {
        return actor instanceof final ServerPlayer player
               && player.hasPermissions(requiredLevel);
    }

}
