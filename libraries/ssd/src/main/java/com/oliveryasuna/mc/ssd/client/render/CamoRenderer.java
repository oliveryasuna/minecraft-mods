package com.oliveryasuna.mc.ssd.client.render;

import com.oliveryasuna.commons.language.exception.UnsupportedInstantiationException;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Predicate;

/**
 * Emits the quads of the camouflaged (disguise) block onto the display's mesh,
 * so a display renders as whatever block it has been skinned to look like.
 */
public final class CamoRenderer {

    //==================================================
    // Static methods
    //==================================================

    public static void emit(
            final QuadEmitter emitter,
            final BlockState camo,
            final RandomSource random,
            final Predicate<Direction> cullTest
    ) {
        if(camo.isAir()) {
            return;
        }

        final BlockStateModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(camo);

        for(final BlockModelPart part : model.collectParts(random)) {
            part.emitQuads(emitter, cullTest);
        }
    }

    //==================================================
    // Constructors
    //==================================================

    private CamoRenderer() {
        super();

        throw new UnsupportedInstantiationException();
    }

}
