package com.r3ct.quests.screen;

import com.r3ct.quests.data.TopEntry;
import com.r3ct.quests.network.RequestLeaderboardPayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class LeaderboardScreen extends Screen {
    private final int boardType;
    private final List<TopEntry> leftList;
    private final List<TopEntry> rightList;

    private final int boardWidth = 400;
    private final int boardHeight = 240;
    private TopEntry hoveredEntry = null;

    public LeaderboardScreen(int boardType, List<TopEntry> leftList, List<TopEntry> rightList) {
        super(Component.translatable("r3ct.leaderboard.title"));
        this.boardType = boardType;
        this.leftList = leftList;
        this.rightList = rightList;
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.extractTransparentBackground(guiGraphics);

        int leftPos = (this.width - boardWidth) / 2;
        int topPos = (this.height - boardHeight) / 2;
        int midX = leftPos + (boardWidth / 2);

        guiGraphics.fill(leftPos, topPos, leftPos + boardWidth, topPos + boardHeight, 0xFF000000);
        guiGraphics.fill(leftPos + 2, topPos + 2, leftPos + boardWidth - 2, topPos + boardHeight - 2, 0xFF333333);
        guiGraphics.fill(leftPos + 2, topPos + 2, leftPos + boardWidth - 2, topPos + 25, 0xFF1A1A1A);
        guiGraphics.fill(midX - 1, topPos + 25, midX + 1, topPos + boardHeight - 2, 0xFF1A1A1A);

        String title = "§6§l" + Component.translatable(boardType == 0 ? "r3ct.leaderboard.title.quests" : "r3ct.leaderboard.title.rewards").getString();
        guiGraphics.centeredText(this.font, title, midX, topPos + 8, 0xFFFFFFFF);

        String leftTitle = "§f" + Component.translatable(boardType == 0 ? "r3ct.leaderboard.left.quests" : "r3ct.leaderboard.left.rewards").getString();
        String rightTitle = "§f" + Component.translatable("r3ct.leaderboard.right.streak").getString();

        guiGraphics.centeredText(this.font, leftTitle, leftPos + (boardWidth / 4), topPos + 32, 0xFFFFFFFF);
        guiGraphics.centeredText(this.font, rightTitle, leftPos + (3 * boardWidth / 4), topPos + 32, 0xFFFFFFFF);

        hoveredEntry = null;
        renderList(guiGraphics, leftList, leftPos + 15, topPos + 50, mouseX, mouseY);
        renderList(guiGraphics, rightList, midX + 15, topPos + 50, mouseX, mouseY);

        if (hoveredEntry != null) {
            java.util.List<net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent> tt = new java.util.ArrayList<>();

            String questsStr = Component.translatable("r3ct.leaderboard.tooltip.quests_completed").getString();
            String maxQStr = Component.translatable("r3ct.leaderboard.tooltip.max_quest_streak").getString();
            String rewardsStr = Component.translatable("r3ct.leaderboard.tooltip.rewards_collected").getString();
            String maxRStr = Component.translatable("r3ct.leaderboard.tooltip.max_reward_streak").getString();
            String daysStr = Component.translatable("r3ct.leaderboard.tooltip.days").getString();

            tt.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(Component.literal("     §f§l" + hoveredEntry.name()).getVisualOrderText()));
            tt.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(Component.literal("§8----------------").getVisualOrderText()));
            tt.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(Component.literal("§f" + questsStr + ": §a" + hoveredEntry.totalQuests()).getVisualOrderText()));
            tt.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(Component.literal("§f" + maxQStr + ": §e" + hoveredEntry.maxQuestStreak() + " " + daysStr).getVisualOrderText()));
            tt.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(Component.literal("§f" + rewardsStr + ": §d" + hoveredEntry.totalRewards()).getVisualOrderText()));
            tt.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(Component.literal("§f" + maxRStr + ": §e" + hoveredEntry.maxRewardStreak() + " " + daysStr).getVisualOrderText()));

            guiGraphics.tooltip(this.font, tt, mouseX, mouseY, net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner.INSTANCE, null);

            ItemStack head = new ItemStack(Items.PLAYER_HEAD);
            head.set(net.minecraft.core.component.DataComponents.PROFILE, net.minecraft.world.item.component.ResolvableProfile.createUnresolved(hoveredEntry.name()));
            guiGraphics.item(head, mouseX + 11, mouseY - 14);
        }

        String switchText = Component.translatable(boardType == 0 ? "r3ct.leaderboard.button.rewards_next" : "r3ct.leaderboard.button.quests_prev").getString();
        int switchWidth = this.font.width(switchText);
        int switchX = (boardType == 0) ? leftPos + boardWidth + 15 : leftPos - 15 - switchWidth;
        int switchY = (this.height / 2) - 4;

        boolean switchHover = mouseX >= switchX && mouseX <= switchX + switchWidth && mouseY >= switchY - 2 && mouseY <= switchY + 10;
        int switchColor = switchHover ? 0xFFFFFFFF : 0xFFAAAAAA;
        guiGraphics.text(this.font, switchText, switchX, switchY, switchColor, true);

        String backText = Component.translatable("r3ct.leaderboard.button.back").getString();
        int backWidth = this.font.width(backText);
        int backX = midX - (backWidth / 2);
        int backY = topPos + boardHeight + 8;

        boolean backHover = mouseX >= backX - 2 && mouseX <= backX + backWidth + 2 && mouseY >= backY - 2 && mouseY <= backY + 10;
        int backColor = backHover ? 0xFFFF5555 : 0xFFAAAAAA;
        guiGraphics.text(this.font, backText, backX, backY, backColor, true);

        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
        if (event.button() == 0) {
            int mX = (int) event.x();
            int mY = (int) event.y();

            int leftPos = (this.width - boardWidth) / 2;
            int topPos = (this.height - boardHeight) / 2;
            int midX = leftPos + (boardWidth / 2);

            String switchText = Component.translatable(boardType == 0 ? "r3ct.leaderboard.button.rewards_next" : "r3ct.leaderboard.button.quests_prev").getString();
            int switchWidth = this.font.width(switchText);
            int switchX = (boardType == 0) ? leftPos + boardWidth + 15 : leftPos - 15 - switchWidth;
            int switchY = (this.height / 2) - 4;

            if (mX >= switchX && mX <= switchX + switchWidth && mY >= switchY - 2 && mY <= switchY + 10) {
                if (this.minecraft != null && this.minecraft.player != null) {
                    this.minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN, 1.0F));
                    com.r3ct.quests.platform.Services.PLATFORM.sendToServer(new RequestLeaderboardPayload(boardType == 0 ? 1 : 0));
                    return true;
                }
            }

            String backText = Component.translatable("r3ct.leaderboard.button.back").getString();
            int backWidth = this.font.width(backText);
            int backX = midX - (backWidth / 2);
            int backY = topPos + boardHeight + 8;

            if (mX >= backX - 2 && mX <= backX + backWidth + 2 && mY >= backY - 2 && mY <= backY + 10) {
                if (this.minecraft != null && this.minecraft.player != null) {
                    this.minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    this.minecraft.player.connection.sendCommand(boardType == 0 ? "daily quests" : "daily rewards");
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.connection.sendCommand(this.boardType == 0 ? "daily quests" : "daily rewards");
            }
            return true;
        }

        if (com.r3ct.quests.platform.Services.PLATFORM.isQuestKey(event)) {
            this.onClose();
            return true;
        }
        if (com.r3ct.quests.platform.Services.PLATFORM.isRewardKey(event)) {
            this.onClose();
            return true;
        }

        return super.keyPressed(event);
    }

    private void renderList(GuiGraphicsExtractor guiGraphics, List<TopEntry> list, int startX, int startY, int mouseX, int mouseY) {
        for (int i = 0; i < list.size(); i++) {
            TopEntry entry = list.get(i);
            int y = startY + (i * 18);

            String nameColor = (i == 0) ? "§d§l" : (i == 1) ? "§e§l" : (i == 2) ? "§b§l" : "§7";

            int scoreToDisplay = 0;
            if (this.boardType == 0 && startX < this.width / 2) scoreToDisplay = entry.totalQuests();
            else if (this.boardType == 0) scoreToDisplay = entry.maxQuestStreak();
            else if (this.boardType == 1 && startX < this.width / 2) scoreToDisplay = entry.totalRewards();
            else scoreToDisplay = entry.maxRewardStreak();

            String valColor = (i == 0) ? "§d" : (i == 1) ? "§e" : (i == 2) ? "§b" : "§f";

            ItemStack head = new ItemStack(net.minecraft.world.item.Items.PLAYER_HEAD);
            head.set(net.minecraft.core.component.DataComponents.PROFILE, net.minecraft.world.item.component.ResolvableProfile.createUnresolved(entry.name()));
            guiGraphics.item(head, startX, y);

            guiGraphics.text(this.font, "§6" + (i + 1) + ". " + nameColor + entry.name(), startX + 20, y + 4, 0xFFFFFFFF, true);

            String scoreTxt = valColor + scoreToDisplay;
            int scoreWidth = this.font.width(scoreTxt);
            guiGraphics.text(this.font, scoreTxt, startX + 165 - scoreWidth, y + 4, 0xFFFFFFFF, true);

            if (mouseX >= startX && mouseX <= startX + 165 && mouseY >= y && mouseY <= y + 16) {
                hoveredEntry = entry;
                guiGraphics.fill(startX - 2, y - 2, startX + 167, y + 18, 0x22FFFFFF);
            }
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }
}