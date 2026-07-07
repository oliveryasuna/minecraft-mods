package com.oliveryasuna.mc.ssd.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.oliveryasuna.commons.language.exception.UnsupportedInstantiationException;
import com.oliveryasuna.mc.ssd.SSDMod;
import com.oliveryasuna.mc.ssd.block.SSDGrid;
import com.oliveryasuna.mc.ssd.block.entity.SSDBlockEntity;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Debug overlay: when {@code debug.outlineGrids} is enabled, draws a line box
 * around every joined display's full N&times;N footprint. Useful for confirming
 * whether groups actually form and reach the client.
 */
public final class SSDDebugRenderer {

    //==================================================
    // Static methods
    //==================================================

    public static void register() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(SSDDebugRenderer::render);
    }

    private static void render(final WorldRenderContext context) {
        if(!SSDMod.debugOutlineGrids()) {
            return;
        }

        final PoseStack poseStack = context.matrixStack();
        final MultiBufferSource consumers = context.consumers();

        if((poseStack == null) || (consumers == null)) {
            return;
        }

        final Vec3 cam = context.camera().getPosition();
        final VertexConsumer lines = consumers.getBuffer(RenderType.lines());

        poseStack.pushPose();
        poseStack.translate(-cam.x, -cam.y, -cam.z);

        for(final SSDBlockEntity be : SSDBlockEntity.CLIENT_INSTANCES) {
            // Draw once per group, from its controller (top-left) cell.
            if(be.isRemoved() || !be.isGrouped() || !be.isController()) {
                continue;
            }

            final BlockPos origin = be.getBlockPos();
            final Direction facing = be.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
            final BlockPos far = SSDGrid.cell(origin, facing, be.gridWidth() - 1, be.gridHeight() - 1);

            // Inflate slightly so the lines sit just proud of the block faces;
            // drawn exactly on the surface they z-fight with the blocks and
            // shimmer as the camera moves.
            final AABB box = AABB.encapsulatingFullBlocks(origin, far).inflate(0.01);

            ShapeRenderer.renderLineBox(poseStack, lines, box, 0.0F, 1.0F, 1.0F, 1.0F);
        }

        poseStack.popPose();

        if(consumers instanceof final MultiBufferSource.BufferSource bufferSource) {
            bufferSource.endBatch(RenderType.lines());
        }
    }

    //==================================================
    // Constructors
    //==================================================

    private SSDDebugRenderer() {
        super();

        throw new UnsupportedInstantiationException();
    }

}
