package com.oliveryasuna.mc.rubric.fabric.gui;

import com.oliveryasuna.mc.rubric.core.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Objects;

/**
 * Vanilla-widget screen listing every {@link ConfigManager} that shares the
 * same owning mod ID. Shown by the ModMenu integration when a mod registers
 * more than one config; clicking a row opens its YACL screen.
 */
public final class ConfigChooserScreen extends Screen {

    //==================================================
    // Fields
    //==================================================

    private final Screen parent;
    private final List<ConfigManager<?>> managers;

    //==================================================
    // Constructors
    //==================================================

    public ConfigChooserScreen(
            final Screen parent,
            final String modId,
            final List<ConfigManager<?>> managers
    ) {
        super(Component.literal(modId));

        this.parent = parent;
        this.managers = List.copyOf(Objects.requireNonNull(managers, "managers"));
    }

    //==================================================
    // Methods
    //==================================================

    @Override
    protected void init() {
        // Vertical stack of one button per config plus a trailing "Done" that
        // returns to ModMenu. Centered, fixed button width — matches the visual
        // weight of vanilla pause/options menus.
        final int buttonWidth = 220;
        final int buttonHeight = 20;
        final int spacing = 4;
        final int x = (this.width - buttonWidth) / 2;
        int y = this.height / 4;
        for(final ConfigManager<?> manager : managers) {
            addRenderableWidget(Button.builder(
                            Component.literal(manager.getSchema().name()),
                            btn -> {
                                final Minecraft client = Objects.requireNonNullElse(this.minecraft, Minecraft.getInstance());
                                client.setScreen(RubricGui.openFor(client, this, manager));
                            })
                    .bounds(x, y, buttonWidth, buttonHeight)
                    .build());
            y += buttonHeight + spacing;
        }
        // Pad before Done so it reads as a distinct row.
        y += spacing * 2;
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, btn -> onClose())
                .bounds(x, y, buttonWidth, buttonHeight)
                .build());
    }

    @Override
    public void onClose() {
        Objects.requireNonNullElse(this.minecraft, Minecraft.getInstance()).setScreen(parent);
    }

}
