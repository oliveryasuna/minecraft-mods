package com.oliveryasuna.mc.ssd.block.entity;

import com.oliveryasuna.commons.language.exception.UnsupportedInstantiationException;
import com.oliveryasuna.mc.ssd.SSDMod;
import com.oliveryasuna.mc.ssd.content.SSDBlocks;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Registry holder for the SSD {@link BlockEntityType}, shared by the digit and
 * hex display blocks. {@link #register()} must run during common mod init,
 * after the display blocks are constructed.
 */
public final class SSDBlockEntities {

    //==================================================
    // Static fields
    //==================================================

    public static BlockEntityType<SSDBlockEntity> SSD;

    //==================================================
    // Static methods
    //==================================================

    public static void register() {
        SSD = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                SSDMod.id("seven_segment_display"),
                FabricBlockEntityTypeBuilder.create(SSDBlockEntity::new, SSDBlocks.DIGIT, SSDBlocks.HEX).build()
        );
    }

    //==================================================
    // Constructors
    //==================================================

    private SSDBlockEntities() {
        super();

        throw new UnsupportedInstantiationException();
    }

}
