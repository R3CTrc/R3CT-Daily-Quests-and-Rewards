package com.r3ct.quests.screen;

import com.r3ct.quests.config.R3CTQuestsConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class R3CTConfigScreen extends Screen {
    private final Screen parent;
    private Button enableHudButton;
    private EditBox xOffsetBox;
    private EditBox yOffsetBox;

    public R3CTConfigScreen(Screen parent) {
        super(Component.translatable("r3ct.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int rightColumnX = this.width / 2 + 10;
        int widgetWidth = 140;
        int widgetHeight = 20;

        this.enableHudButton = Button.builder(
                getHudButtonText(),
                button -> {
                    R3CTQuestsConfig.getInstance().enableHud = !R3CTQuestsConfig.getInstance().enableHud;
                    button.setMessage(getHudButtonText());
                }
        ).bounds(rightColumnX, 50, widgetWidth, widgetHeight).build();
        this.addRenderableWidget(this.enableHudButton);

        this.xOffsetBox = new EditBox(this.font, rightColumnX, 80, widgetWidth, widgetHeight, Component.translatable("r3ct.config.entry.hud_x"));
        this.xOffsetBox.setValue(String.valueOf(R3CTQuestsConfig.getInstance().hudXOffset));
        this.addRenderableWidget(this.xOffsetBox);

        this.yOffsetBox = new EditBox(this.font, rightColumnX, 110, widgetWidth, widgetHeight, Component.translatable("r3ct.config.entry.hud_y"));
        this.yOffsetBox.setValue(String.valueOf(R3CTQuestsConfig.getInstance().hudYOffset));
        this.addRenderableWidget(this.yOffsetBox);

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
                .bounds(this.width / 2 - 100, this.height - 40, 200, 20).build());
    }

    private Component getHudButtonText() {
        boolean isEnabled = R3CTQuestsConfig.getInstance().enableHud;

        if (isEnabled) {
            return Component.literal("ON").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD);
        } else {
            return Component.literal("OFF").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, 0x99000000);

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFFFF);

        int leftColumnX = this.width / 2 - 150;

        guiGraphics.drawString(this.font, Component.translatable("r3ct.config.entry.enable_hud"), leftColumnX, 50 + 6, 0xFFFFFFFF);
        guiGraphics.drawString(this.font, Component.translatable("r3ct.config.entry.hud_x"), leftColumnX, 80 + 6, 0xFFFFFFFF);
        guiGraphics.drawString(this.font, Component.translatable("r3ct.config.entry.hud_y"), leftColumnX, 110 + 6, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        try {
            R3CTQuestsConfig.getInstance().hudXOffset = Integer.parseInt(this.xOffsetBox.getValue());
        } catch (NumberFormatException ignored) {}

        try {
            R3CTQuestsConfig.getInstance().hudYOffset = Integer.parseInt(this.yOffsetBox.getValue());
        } catch (NumberFormatException ignored) {}

        R3CTQuestsConfig.save();

        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }
}