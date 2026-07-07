package com.oliveryasuna.mc.ssd.block.entity;

import com.oliveryasuna.commons.language.exception.UnsupportedInstantiationException;
import com.oliveryasuna.mc.ssd.SSDMod;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
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
                ResourceLocation.fromNamespaceAndPath(SSDMod.MOD_ID, "seven_segment_display"),
                FabricBlockEntityTypeBuilder.create(SSDBlockEntity::new, SSDMod.DIGIT_BLOCK, SSDMod.HEX_BLOCK).build()
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
