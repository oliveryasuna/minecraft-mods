package com.oliveryasuna.mc.ssd.content;

import com.oliveryasuna.commons.language.exception.UnsupportedInstantiationException;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.world.item.CreativeModeTabs;

/**
 * Adds the display items to a vanilla creative tab.
 */
public final class SSDCreativeTab {

    //==================================================
    // Static methods
    //==================================================

    public static void register() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.REDSTONE_BLOCKS)
                .register(entries -> {
                    entries.accept(SSDItems.DIGIT);
                    entries.accept(SSDItems.HEX);
                });
    }

    //==================================================
    // Constructors
    //==================================================

    private SSDCreativeTab() {
        super();

        throw new UnsupportedInstantiationException();
    }

}
