package com.r3ct.daily.client.screen;

import com.r3ct.daily.logic.Quest;
import com.r3ct.daily.network.SubmitQuestItemPayload;
import com.r3ct.daily.platform.Services;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

public class ConfirmSubmitScreen extends Screen {
    private final Screen parent;
    private final Quest quest;
    private final int questIndex;
    private final int slotIndex;
    private final int amountToTake;

    public ConfirmSubmitScreen(Screen parent, Quest quest, int questIndex, int slotIndex, int amountToTake) {
        super(Component.translatable("r3ct_daily.gui.confirm_submit"));
        this.parent = parent;
        this.quest = quest;
        this.questIndex = questIndex;
        this.slotIndex = slotIndex;
        this.amountToTake = amountToTake;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        this.addRenderableWidget(Button.builder(Component.translatable("r3ct_daily.gui.yes"), b -> {
            Services.PLATFORM.sendToServer(new SubmitQuestItemPayload(questIndex, slotIndex));
            this.minecraft.setScreen(parent);
        }).bounds(centerX - 105, centerY + 30, 100, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("r3ct_daily.gui.no"), b -> {
            this.minecraft.setScreen(parent);
        }).bounds(centerX + 5, centerY + 30, 100, 20).build());
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, 0xD9000000);
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

        Item targetItem = BuiltInRegistries.ITEM.getOptional(Identifier.parse(quest.target)).orElse(net.minecraft.world.item.Items.PAPER);
        ItemStack displayStack = new ItemStack(targetItem, amountToTake);

        guiGraphics.centeredText(this.font, Component.translatable("r3ct_daily.gui.submit_question", amountToTake, displayStack.getHoverName()), this.width / 2, this.height / 2 - 40, 0xFFFFFFFF);

        guiGraphics.item(displayStack, this.width / 2 - 8, this.height / 2 - 15);
        guiGraphics.itemDecorations(this.font, displayStack, this.width / 2 - 8, this.height / 2 - 15);
    }
}