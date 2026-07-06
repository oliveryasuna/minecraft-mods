package com.oliveryasuna.mc.ssd.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

/**
 * {@link BlockItem} for the seven-segment display that documents the (non-obvious) redstone-to-digit
 * mapping in its hover tooltip: signal 0 blanks the display, and signals 1-10 show digits 0-9.
 */
public final class SSDBlockItem extends BlockItem {

    //==================================================
    // Constructors
    //==================================================

    public SSDBlockItem(final Block block, final Properties properties) {
        super(block, properties);
    }

    //==================================================
    // Methods
    //==================================================

    // Item
    //--------------------------------------------------

    @Override
    public void appendHoverText(
            final ItemStack stack,
            final Item.TooltipContext context,
            final TooltipDisplay tooltipDisplay,
            final Consumer<Component> tooltipAdder,
            final TooltipFlag tooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltipAdder, tooltipFlag);

        tooltipAdder.accept(Component.translatable("tooltip.seven-segment-display.seven_segment_display.header")
                .withStyle(ChatFormatting.GRAY));
        tooltipAdder.accept(Component.translatable("tooltip.seven-segment-display.seven_segment_display.blank")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltipAdder.accept(Component.translatable("tooltip.seven-segment-display.seven_segment_display.digits")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

}