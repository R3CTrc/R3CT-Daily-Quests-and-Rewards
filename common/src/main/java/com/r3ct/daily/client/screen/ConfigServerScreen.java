package com.r3ct.daily.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

import java.io.File;
import java.nio.file.Paths;

public class ConfigServerScreen extends Screen {
    private final Screen parent;

    public ConfigServerScreen(Screen parent) {
        super(Component.translatable("r3ct.config.server.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int buttonWidth = 200;
        int buttonHeight = 20;
        int centerX = this.width / 2 - buttonWidth / 2;
        int startY = this.height / 2 - 50;

        this.addRenderableWidget(Button.builder(Component.translatable("r3ct.config.server.button.quests"), button -> openFile("r3ct_daily_quests.json"))
                .bounds(centerX, startY, buttonWidth, buttonHeight).build());

        this.addRenderableWidget(Button.builder(Component.translatable("r3ct.config.server.button.rewards"), button -> openFile("r3ct_daily_rewards.json"))
                .bounds(centerX, startY + 25, buttonWidth, buttonHeight).build());

        this.addRenderableWidget(Button.builder(Component.translatable("r3ct.config.server.button.quests_rewards"), button -> openFile("r3ct_daily_quests_rewards.json"))
                .bounds(centerX, startY + 50, buttonWidth, buttonHeight).build());

        this.addRenderableWidget(Button.builder(Component.translatable("r3ct.config.server.button.mechanics"), button -> openFile("r3ct_daily_server.json"))
                .bounds(centerX, startY + 75, buttonWidth, buttonHeight).build());

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
                .bounds(centerX, startY + 110, buttonWidth, buttonHeight).build());
    }

    private void openFile(String fileName) {
        File configFile = Paths.get("config", "r3ct_daily", fileName).toFile();
        if (configFile.exists()) {
            Util.getPlatform().openUri(configFile.toURI());
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, 0x99000000);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }
}