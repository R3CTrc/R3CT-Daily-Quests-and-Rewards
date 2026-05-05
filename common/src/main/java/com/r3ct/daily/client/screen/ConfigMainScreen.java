package com.r3ct.daily.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

public class ConfigMainScreen extends Screen {
    private final Screen parent;

    public ConfigMainScreen(Screen parent) {
        super(Component.translatable("r3ct.config.main.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int buttonWidth = 200;
        int buttonHeight = 20;
        int centerX = this.width / 2 - buttonWidth / 2;
        int startY = this.height / 2 - 30;

        this.addRenderableWidget(Button.builder(Component.translatable("r3ct.config.main.client_button"), button -> {
                    this.minecraft.setScreen(new ConfigClientScreen(this));
                })
                .bounds(centerX, startY, buttonWidth, buttonHeight)
                .tooltip(Tooltip.create(Component.translatable("r3ct.config.main.client_tooltip")))
                .build());

        boolean isSingleplayer = this.minecraft != null && this.minecraft.hasSingleplayerServer();

        Button serverButton = Button.builder(Component.translatable("r3ct.config.main.server_button"), button -> {
                    this.minecraft.setScreen(new ConfigServerScreen(this));
                })
                .bounds(centerX, startY + 25, buttonWidth, buttonHeight)
                .tooltip(Tooltip.create(
                        isSingleplayer
                                ? Component.translatable("r3ct.config.main.server_tooltip")
                                : Component.translatable("r3ct.config.main.server_tooltip_disabled").withStyle(ChatFormatting.RED)
                ))
                .build();

        serverButton.active = isSingleplayer;
        this.addRenderableWidget(serverButton);

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
                .bounds(centerX, startY + 60, buttonWidth, buttonHeight).build());
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, 0x99000000);
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.centeredText(this.font, this.title, this.width / 2, 20, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }
}