package com.oliveryasuna.mc.rubric.fabric.gui;

import com.oliveryasuna.mc.rubric.api.ConfigManager;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Fallback screen shown when no {@link ScreenProvider} is registered (e.g.,
 * YACL is not installed). Explains the situation and points the user at the
 * config file on disk so they can still edit by hand.
 */
public final class NoFrontendScreen extends Screen {

    //==================================================
    // Fields
    //==================================================

    private final Screen parent;
    private final ConfigManager<?> manager;

    //==================================================
    // Constructors
    //==================================================

    public NoFrontendScreen(
            final Screen parent,
            final ConfigManager<?> manager
    ) {
        super(Component.literal(Objects.requireNonNull(manager, "manager").getSchema().name()));

        this.parent = parent;
        this.manager = manager;
    }

    //==================================================
    // Methods
    //==================================================

    @Override
    protected void init() {
        final int contentWidth = Math.min(this.width - 40, 380);
        final int x = (this.width - contentWidth) / 2;
        final int y = this.height / 4;

        // Body text: blame the missing frontend, name an example (YACL), point
        // at the on-disk file so users can still edit by hand.
        final Component body = Component.literal(
                "No config GUI frontend is installed.\n\n"
                + "Install YetAnotherConfigLib (YACL) to edit this config in-game.\n\n"
                + "Meanwhile, the config file is at:\n"
                + manager.getFile());
        final MultiLineTextWidget text = new MultiLineTextWidget(x, y, body, this.font)
                .setMaxWidth(contentWidth)
                .setCentered(true);
        addRenderableWidget(text);

        final int buttonWidth = 200;
        final int buttonHeight = 20;
        final int spacing = 4;
        final int buttonX = (this.width - buttonWidth) / 2;
        // Stack three buttons bottom-up so "Done" sits at the screen-edge
        // anchor users expect, with the file actions immediately above.
        int buttonY = this.height - 30 - buttonHeight;
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, btn -> onClose())
                .bounds(buttonX, buttonY, buttonWidth, buttonHeight)
                .build());
        final Path file = manager.getFile();
        final Path folder = file.getParent();
        buttonY -= buttonHeight + spacing;
        addRenderableWidget(Button.builder(
                        Component.literal("Open Config"),
                        btn -> Util.getPlatform().openFile(file.toFile()))
                .bounds(buttonX, buttonY, buttonWidth, buttonHeight)
                .build());
        buttonY -= buttonHeight + spacing;
        addRenderableWidget(Button.builder(
                        Component.literal("Open Folder"),
                        btn -> Util.getPlatform().openFile(folder.toFile()))
                .bounds(buttonX, buttonY, buttonWidth, buttonHeight)
                .build());
    }

    @Override
    public void render(
            final GuiGraphics graphics,
            final int mouseX,
            final int mouseY,
            final float partialTick
    ) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        Objects.requireNonNullElse(this.minecraft, Minecraft.getInstance()).setScreen(parent);
    }

}
