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
 * {@link BlockItem} for a seven-segment display that documents its
 * redstone-to-glyph mapping in the hover tooltip. The two lines are keyed off
 * the block's description id, so the digit and hex displays show their own
 * text.
 */
public final class SSDBlockItem extends BlockItem {

    //==================================================
    // Constructors
    //==================================================

    public SSDBlockItem(
            final Block block,
            final Properties properties
    ) {
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

        // e.g. "block.seven-segment-display.digit_display.tip.header" /
        // ".mapping".
        final String base = getDescriptionId() + ".tip";

        tooltipAdder.accept(Component.translatable(base + ".header").withStyle(ChatFormatting.GRAY));
        tooltipAdder.accept(Component.translatable(base + ".mapping").withStyle(ChatFormatting.DARK_GRAY));
    }

}
