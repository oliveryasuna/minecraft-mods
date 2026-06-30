package com.oliveryasuna.mc.omniconfig.fabric;

import com.oliveryasuna.mc.omniconfig.platform.PermissionGate;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fabric implementation of {@link PermissionGate}. Mojmap names.
 */
public final class FabricPermissionGate implements PermissionGate {

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
