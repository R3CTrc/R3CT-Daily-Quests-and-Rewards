package com.r3ct.daily.client.screen;

import com.r3ct.daily.logic.Quest;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class ItemSelectionScreen extends Screen {
    private final Screen parent;
    private final Quest quest;
    private final int questIndex;
    private final List<Integer> slots;
    private final int amountToTake;

    public ItemSelectionScreen(Screen parent, Quest quest, int questIndex, List<Integer> slots, int amountToTake) {
        super(Component.translatable("r3ct_daily.gui.select_item"));
        this.parent = parent;
        this.quest = quest;
        this.questIndex = questIndex;
        this.slots = slots;
        this.amountToTake = amountToTake;
    }

    @Override
    protected void init() {
        this.addRenderableWidget(Button.builder(net.minecraft.network.chat.CommonComponents.GUI_CANCEL, button -> {
            if (this.minecraft != null) this.minecraft.setScreen(this.parent);
        }).bounds(this.width / 2 - 50, this.height / 2 + 50, 100, 20).build());
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, 0xD9000000);
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.centeredText(this.font, this.title, this.width / 2, this.height / 2 - 50, 0xFFFFFFFF);

        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        int slotSize = 24;
        int spacing = 4;
        int step = slotSize + spacing;

        int totalWidth = (slots.size() * step) - spacing;

        int startX = (this.width - totalWidth) / 2;
        int startY = this.height / 2 - (slotSize / 2);

        for (int i = 0; i < slots.size(); i++) {
            int slot = slots.get(i);
            int slotX = startX + (i * step);
            int slotY = startY;

            guiGraphics.fill(slotX, slotY, slotX + slotSize, slotY + slotSize, 0x66000000);

            ItemStack stack = player.getInventory().getItem(slot);
            int itemX = slotX + 4;
            int itemY = slotY + 4;

            guiGraphics.item(stack, itemX, itemY);
            guiGraphics.itemDecorations(this.font, stack, itemX, itemY);

            if (mouseX >= slotX && mouseX <= slotX + slotSize && mouseY >= slotY && mouseY <= slotY + slotSize) {
                guiGraphics.fill(slotX, slotY, slotX + slotSize, slotY + slotSize, 0x44FFFFFF);
                guiGraphics.setTooltipForNextFrame(this.font, stack.getTooltipLines(net.minecraft.world.item.Item.TooltipContext.of(this.minecraft.level), this.minecraft.player, net.minecraft.world.item.TooltipFlag.NORMAL), java.util.Optional.empty(), mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();

        int slotSize = 24;
        int spacing = 4;
        int step = slotSize + spacing;
        int totalWidth = (slots.size() * step) - spacing;
        int startX = (this.width - totalWidth) / 2;
        int startY = this.height / 2 - (slotSize / 2);

        Player player = Minecraft.getInstance().player;

        for (int i = 0; i < slots.size(); i++) {
            int slotX = startX + (i * step);
            int slotY = startY;

            if (mouseX >= slotX && mouseX <= slotX + slotSize && mouseY >= slotY && mouseY <= slotY + slotSize) {
                if (player != null) {
                    int slot = slots.get(i);
                    ItemStack stack = player.getInventory().getItem(slot);
                    int actualTake = Math.min(amountToTake, stack.getCount());
                    this.minecraft.setScreen(new ConfirmSubmitScreen(this.parent, quest, questIndex, slot, actualTake));
                }
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }
}