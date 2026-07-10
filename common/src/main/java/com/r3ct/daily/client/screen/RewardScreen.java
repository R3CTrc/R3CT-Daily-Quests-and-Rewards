package com.r3ct.daily.client.screen;

import com.r3ct.daily.config.DailyClientConfig;
import com.r3ct.daily.config.DailyServerConfig;
import com.r3ct.daily.logic.QuestManager;
import com.r3ct.daily.network.OpenRewardsPayload;
import com.r3ct.daily.platform.Services;
import com.r3ct.daily.data.PlayerData;
import com.r3ct.daily.network.RequestLeaderboardPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.NonNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RewardScreen extends Screen {
    private final PlayerData data;
    private final OpenRewardsPayload payload;

    private final int imageWidth = 330;
    private final int imageHeight = 200;

    private static float animatedStreak = 0.0f;
    private static float animatedCollected = 0.0f;

    public RewardScreen(PlayerData data, OpenRewardsPayload payload) {
        super(Component.translatable("r3ct_daily.rewards.title"));
        this.data = data;
        this.payload = payload;
    }

    private String getCurrentQuestDateString() {
        return LocalDateTime.now().minusHours(payload.questRefreshHour()).toLocalDate().toString();
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

        float scale = DailyClientConfig.getInstance().rewardScreenScale;

        mouseX = (int)((mouseX - this.width / 2f) / scale + this.width / 2f);
        mouseY = (int)((mouseY - this.height / 2f) / scale + this.height / 2f);

        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(this.width / 2f, this.height / 2f);
        guiGraphics.pose().scale(scale, scale);
        guiGraphics.pose().translate(-this.width / 2f, -this.height / 2f);

        int leftPos = (this.width - imageWidth) / 2;
        int topPos = (this.height - imageHeight) / 2;

        guiGraphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF000000);
        guiGraphics.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2, topPos + imageHeight - 2, 0xFFC6C6C6);
        guiGraphics.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2, topPos + 4, 0xFFFFFFFF);
        guiGraphics.fill(leftPos + 2, topPos + 2, leftPos + 4, topPos + imageHeight - 2, 0xFFFFFFFF);
        guiGraphics.fill(leftPos + imageWidth - 4, topPos + 2, leftPos + imageWidth - 2, topPos + imageHeight - 2, 0xFF555555);
        guiGraphics.fill(leftPos + 2, topPos + imageHeight - 4, leftPos + imageWidth - 2, topPos + imageHeight - 2, 0xFF555555);

        String headerText = "§l" + Component.translatable("r3ct_daily.rewards.header.rewards").getString();
        guiGraphics.text(this.font, headerText, (this.width / 2) - (this.font.width(headerText) / 2), topPos + 12, 0xFF404040, false);

        guiGraphics.fill(leftPos + 12, topPos + 30, leftPos + imageWidth - 12, topPos + 90, 0xFF8B8B8B);

        int hoveredDay = -1;
        String dzisiajData = getCurrentQuestDateString();
        boolean juzDzisiajOdebrane = dzisiajData.equals(data.lastRewardDate);

        for (int i = 1; i <= 7; i++) {
            int slotX = leftPos + 27 + (i - 1) * 43;
            int slotY = topPos + 43;

            boolean czyOdebrane = juzDzisiajOdebrane ? ((data.rewardDay == 1) || (i < data.rewardDay)) : (i < data.rewardDay);
            boolean czyMoznaOdebrac = (i == data.rewardDay) && !juzDzisiajOdebrane;

            guiGraphics.fill(slotX, slotY, slotX + 18, slotY + 18, czyMoznaOdebrac ? 0xFFFFAA00 : 0xFF8B8B8B);
            guiGraphics.fill(slotX, slotY, slotX + 17, slotY + 1, 0xFF373737);
            guiGraphics.fill(slotX, slotY, slotX + 1, slotY + 17, 0xFF373737);
            guiGraphics.fill(slotX + 1, slotY + 17, slotX + 18, slotY + 18, 0xFFFFFFFF);
            guiGraphics.fill(slotX + 17, slotY + 1, slotX + 18, slotY + 18, 0xFFFFFFFF);

            if (isMouseOverSlot(mouseX, mouseY, slotX, slotY)) {
                guiGraphics.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, 0x80FFFFFF);
                hoveredDay = i;
            }

            ItemStack icon = getIconForDay(i);
            guiGraphics.item(icon, slotX + 1, slotY + 1);
            guiGraphics.itemDecorations(this.font, icon, slotX + 1, slotY + 1);

            if (czyMoznaOdebrac && data.streak >= 7) {
                guiGraphics.text(this.font, "§6§l+", slotX + 12, slotY + 12, 0xFFFFFFFF, true);
            }

            int kolorTekstu;
            if (czyOdebrane) {
                kolorTekstu = 0xFF707070;
            } else {
                if (i <= 4) kolorTekstu = 0xFFFFFF55;
                else if (i <= 6) kolorTekstu = 0xFF55FFFF;
                else kolorTekstu = 0xFFFF55FF;
            }

            String dayTxt = Component.translatable("r3ct_daily.rewards.day", i).getString();
            guiGraphics.text(this.font, dayTxt, (slotX + 9) - (this.font.width(dayTxt) / 2), slotY + 25, kolorTekstu, true);

            if (czyMoznaOdebrac) {
                String arrow = "▲";
                guiGraphics.text(this.font, arrow, (slotX + 9) - (this.font.width(arrow) / 2), slotY + 37, 0xFF55FF55, true);
            }

            if (czyOdebrane) guiGraphics.text(this.font, "§a✔", slotX + 11, slotY + 11, 0xFFFFFFFF, true);
        }

        int barY = topPos + 140;
        int bar1X = leftPos + 18;
        int bWidth = 135;

        int targetStreak = Math.min(data.streak, 7);
        animatedStreak += (targetStreak - animatedStreak) * 0.1f;
        if (Math.abs(targetStreak - animatedStreak) < 0.05f) animatedStreak = targetStreak;

        String streakColorCode = (targetStreak < 3) ? "§2" : (targetStreak < 7 ? "§6" : "§c");
        int sColor = (targetStreak < 3) ? 0xFF006400 : (targetStreak < 7 ? 0xFFFFAA00 : 0xFFFF5555);

        String lblStreak = Component.translatable("r3ct_daily.rewards.bar.streak").getString();
        String valStreak = streakColorCode + data.streak + "§0/7";
        GuiUtils.drawRewardStyleBar(guiGraphics, this.font, bar1X, barY, animatedStreak, 7, lblStreak, valStreak, sColor, bWidth, 1, new int[]{});

        String multiText = data.streak >= 7 ? "§6§l" + Component.translatable("r3ct_daily.rewards.bonus.active").getString() : "§8" + Component.translatable("r3ct_daily.rewards.bonus.inactive").getString();
        guiGraphics.text(this.font, multiText, bar1X, barY + 12, 0xFFFFFFFF, data.streak >= 7);

        int displayCollected = data.totalCollected % 21;
        if (displayCollected == 0 && data.totalCollected > 0) {
            displayCollected = juzDzisiajOdebrane ? 21 : 0;
        }

        animatedCollected += (displayCollected - animatedCollected) * 0.1f;
        if (Math.abs(displayCollected - animatedCollected) < 0.05f) animatedCollected = displayCollected;

        int bar2X = leftPos + 173;

        String lblBonus = Component.translatable("r3ct_daily.rewards.bar.bonus").getString();
        String valBonus = "§d" + displayCollected + "§0/21";
        GuiUtils.drawRewardStyleBar(guiGraphics, this.font, bar2X, barY, animatedCollected, 21, lblBonus, valBonus, 0xFFFF55FF, bWidth, 1, new int[]{});

        renderBonusMilestones(guiGraphics, bar2X, barY, bWidth, mouseX, mouseY);

        String arrowText = Component.translatable("r3ct_daily.leaderboard.button.quests_prev").getString();
        int textWidth = this.font.width(arrowText);
        int arrowX = leftPos - 15 - textWidth;
        int arrowY = (this.height / 2) - 4;

        boolean isHovered = mouseX >= arrowX && mouseX <= arrowX + textWidth && mouseY >= arrowY - 2 && mouseY <= arrowY + 10;
        int color = isHovered ? 0xFFFFFFFF : 0xFFAAAAAA;

        guiGraphics.text(this.font, arrowText, arrowX, arrowY, color, true);

        int trophyX = leftPos + imageWidth - 24;
        int trophyY = topPos + 6;
        boolean trophyHover = mouseX >= trophyX && mouseX <= trophyX + 16 && mouseY >= trophyY && mouseY <= trophyY + 16;

        if (trophyHover) guiGraphics.fill(trophyX - 2, trophyY - 2, trophyX + 18, trophyY + 18, 0x44FFFFFF);
        guiGraphics.item(new ItemStack(Items.MOJANG_BANNER_PATTERN), trophyX, trophyY);

        int midX = leftPos + (imageWidth / 2);
        String backText = Component.translatable("r3ct_daily.quests.button.close").getString();
        int backWidth = this.font.width(backText);
        int backX = midX - (backWidth / 2);
        int backY = topPos + imageHeight + 8;

        boolean backHover = mouseX >= backX - 2 && mouseX <= backX + backWidth + 2 && mouseY >= backY - 2 && mouseY <= backY + 10;
        int backColor = backHover ? 0xFFFF5555 : 0xFFAAAAAA;
        guiGraphics.text(this.font, backText, backX, backY, backColor, true);

        if (hoveredDay != -1) {
            renderDayTooltip(guiGraphics, hoveredDay, getIconForDay(hoveredDay), mouseX, mouseY);
        }

        if (mouseX >= bar1X && mouseX <= bar1X + bWidth && mouseY >= barY && mouseY <= barY + 10) {
            renderStreakTooltip(guiGraphics, mouseX, mouseY);
        }

        if (mouseX >= bar2X && mouseX <= bar2X + bWidth && mouseY >= barY && mouseY <= barY + 10) {
            renderBonusTooltip(guiGraphics, data.totalCollected, mouseX, mouseY);
        }

        int[] thresholds = {7, 14, 21};
        String rewLabel = Component.translatable("r3ct_daily.quests.tooltip.reward").getString();

        DailyServerConfig.MilestoneReward[] mr = {
                DailyServerConfig.bonuses.bonus_7,
                DailyServerConfig.bonuses.bonus_14,
                DailyServerConfig.bonuses.bonus_21
        };

        String[] titles = new String[3];
        String[] rewards = new String[3];
        String[] amountsStr = new String[3];

        for (int i = 0; i < 3; i++) {
            String rewardColor = mr[i].getFormattedColor();

            titles[i] = rewardColor + "§l" + Component.translatable("r3ct_daily.rewards.tooltip.bonus.title", thresholds[i]).getString();
            ItemStack stack = QuestManager.getMilestoneRewardStack(mr[i]);
            rewards[i] = "§f" + rewLabel + " " + rewardColor + mr[i].amount + "x " + stack.getHoverName().getString();
            amountsStr[i] = rewardColor + "x" + mr[i].amount;
        }

        for (int i = 0; i < 3; i++) {
            int t = thresholds[i];
            int mX = (t == 21) ? (bar2X + bWidth - 1) : (bar2X + (int)(t * (bWidth / 21.0)));
            int totalW = 18 + this.font.width(amountsStr[i]);
            int startX = mX - totalW / 2;
            int startY = barY + 24;

            if (mouseX >= startX - 2 && mouseX <= startX + totalW + 2 && mouseY >= startY - 2 && mouseY <= startY + 18) {
                renderSimpleTooltip(guiGraphics, titles[i], rewards[i], mouseX, mouseY);
            }
        }

        if (trophyHover) {
            List<ClientTooltipComponent> tTooltip = new ArrayList<>();
            tTooltip.add(ClientTooltipComponent.create(Component.literal("§6§l" + Component.translatable("r3ct_daily.quests.tooltip.leaderboard.title").getString()).getVisualOrderText()));
            tTooltip.add(ClientTooltipComponent.create(Component.literal("§8----------------").getVisualOrderText()));
            tTooltip.add(ClientTooltipComponent.create(Component.literal("§f" + Component.translatable("r3ct_daily.quests.tooltip.leaderboard.desc").getString()).getVisualOrderText()));
            guiGraphics.tooltip(this.font, tTooltip, mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null);
        }

        guiGraphics.pose().popMatrix();
    }

    private void renderBonusMilestones(GuiGraphicsExtractor g, int x, int y, int bWidth, int mouseX, int mouseY) {
        int[] thresholds = {7, 14, 21};
        DailyServerConfig.MilestoneReward[] mr = {
                DailyServerConfig.bonuses.bonus_7,
                DailyServerConfig.bonuses.bonus_14,
                DailyServerConfig.bonuses.bonus_21
        };

        ItemStack[] icons = new ItemStack[3];
        String[] amounts = new String[3];

        for (int i = 0; i < 3; i++) {
            icons[i] = QuestManager.getMilestoneRewardStack(mr[i]);
            amounts[i] = mr[i].getFormattedColor() + "x" + mr[i].amount;
        }

        long time = System.currentTimeMillis();

        int cycle = (data.totalCollected == 0) ? 0 : (data.totalCollected - 1) / 21;

        for (int i = 0; i < 3; i++) {
            int t = thresholds[i];
            int absTarget = cycle * 21 + t;
            boolean canClaim = data.totalCollected >= absTarget && !data.claimedBonusRewards.contains(absTarget);
            boolean claimed = data.claimedBonusRewards.contains(absTarget);

            int mX = (t == 21) ? (x + bWidth - 1) : (x + (int)(t * (bWidth / 21.0)));

            g.fill(mX, y - 2, mX + 1, y + 10, 0xFFFFFFFF);
            String txtDay = "§8" + t;
            g.text(this.font, txtDay, mX - (this.font.width(String.valueOf(t)) / 2), y + 12, 0xFFFFFFFF, false);

            int totalW = 18 + this.font.width(amounts[i]);
            int startX = mX - totalW / 2;
            int startY = y + 24;

            boolean hovered = mouseX >= startX - 2 && mouseX <= startX + totalW + 2 && mouseY >= startY - 2 && mouseY <= startY + 18;

            if (claimed) {
                g.fill(startX - 2, startY - 2, startX + totalW + 2, startY + 18, 0x44000000);
            } else if (canClaim) {
                int color = hovered ? 0x66FFFF00 : 0x44FFFF00;
                g.fill(startX - 2, startY - 2, startX + totalW + 2, startY + 18, color);
            } else if (hovered) {
                g.fill(startX - 2, startY - 2, startX + totalW + 2, startY + 18, 0x22000000);
            }

            g.item(icons[i], startX, startY);
            g.text(this.font, amounts[i], startX + 18, startY + 4, 0xFF000000, false);

            if (claimed) {
                g.pose().pushMatrix();
                g.pose().translate(0, 0);
                g.text(this.font, "§a✔", startX + 10, startY + 8, 0xFFFFFFFF, true);
                g.pose().popMatrix();
            } else if (canClaim) {
                g.pose().pushMatrix();
                g.pose().translate(mX, startY + 22);
                g.pose().scale(1.5f, 1.5f);
                String arrow = (time % 1000 < 500) ? "§e↑" : "§6↑";
                g.text(this.font, arrow, -this.font.width("↑") / 2, 0, 0xFF000000, true);
                g.pose().popMatrix();
            }
        }
    }

    private void renderDayTooltip(GuiGraphicsExtractor guiGraphics, int day, ItemStack stack, int mouseX, int mouseY) {
        String dzisiajData = getCurrentQuestDateString();
        boolean juzDzisiajOdebrane = dzisiajData.equals(data.lastRewardDate);
        boolean czyOdebrane = juzDzisiajOdebrane ? ((data.rewardDay == 1) || (day < data.rewardDay)) : (day < data.rewardDay);
        boolean czyMoznaOdebrac = (day == data.rewardDay) && !juzDzisiajOdebrane;

        List<ClientTooltipComponent> tooltip = new ArrayList<>();
        tooltip.add(ClientTooltipComponent.create(Component.literal("§6§l" + Component.translatable("r3ct_daily.rewards.tooltip.day.title", day).getString()).getVisualOrderText()));

        String statusRaw = czyOdebrane ? "§a" + Component.translatable("r3ct_daily.rewards.status.claimed").getString() : (czyMoznaOdebrac ? "§e" + Component.translatable("r3ct_daily.rewards.status.claimable").getString() : "§c" + Component.translatable("r3ct_daily.rewards.status.locked").getString());
        tooltip.add(ClientTooltipComponent.create(Component.literal(Component.translatable("r3ct_daily.rewards.tooltip.status").getString() + " " + statusRaw).getVisualOrderText()));

        tooltip.add(ClientTooltipComponent.create(Component.literal("§8----------------").getVisualOrderText()));

        if (czyOdebrane) {
            String zHisto = (data.claimedRewardHistory != null && data.claimedRewardHistory.size() > day - 1) ? data.claimedRewardHistory.get(day - 1) : "";
            if (zHisto != null && !zHisto.isEmpty()) {
                tooltip.add(ClientTooltipComponent.create(Component.literal("§f" + Component.translatable("r3ct_daily.rewards.tooltip.content").getString()).getVisualOrderText()));

                String[] items = zHisto.split(",");
                for (String itemStr : items) {
                    itemStr = itemStr.trim();

                    if (itemStr.contains(";")) {
                        String[] parts = itemStr.split(";", 2);
                        String amount = parts[0];
                        String translationKey = parts[1];

                        MutableComponent line = Component.literal("  - §b" + amount + "x ").append(Component.translatable(translationKey).withStyle(ChatFormatting.AQUA));
                        tooltip.add(ClientTooltipComponent.create(line.getVisualOrderText()));
                    } else {
                        tooltip.add(ClientTooltipComponent.create(Component.literal("  - " + itemStr).getVisualOrderText()));
                    }
                }
            } else {
                tooltip.add(ClientTooltipComponent.create(Component.literal("§f" + Component.translatable("r3ct_daily.rewards.tooltip.content").getString() + " §8(" + Component.translatable("r3ct_daily.rewards.tooltip.no_data").getString() + ")").getVisualOrderText()));
            }
        } else if (day == 7) {
            tooltip.add(ClientTooltipComponent.create(Component.literal("§f" + Component.translatable("r3ct_daily.rewards.tooltip.content").getString() + " §d" + Component.translatable("r3ct_daily.rewards.tooltip.epic_rewards").getString()).getVisualOrderText()));
            tooltip.add(ClientTooltipComponent.create(Component.literal("§b+ 1 " + Component.translatable("r3ct_daily.rewards.tooltip.shield").getString()).getVisualOrderText()));
        } else {
            tooltip.add(ClientTooltipComponent.create(Component.literal("§f" + Component.translatable("r3ct_daily.rewards.tooltip.content").getString() + " §7§k???").getVisualOrderText()));
        }

        if (czyMoznaOdebrac && data.streak >= 7) {
            tooltip.add(ClientTooltipComponent.create(Component.literal("§6★ " + Component.translatable("r3ct_daily.rewards.tooltip.bonus_loot").getString()).getVisualOrderText()));
        }

        guiGraphics.tooltip(this.font, tooltip, mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null);
    }

    private void renderStreakTooltip(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        List<ClientTooltipComponent> tooltip = new ArrayList<>();
        tooltip.add(ClientTooltipComponent.create(Component.literal("§6§l" + Component.translatable("r3ct_daily.rewards.tooltip.streak.title").getString()).getVisualOrderText()));
        tooltip.add(ClientTooltipComponent.create(Component.literal("§8----------------").getVisualOrderText()));
        if (data.streak >= 7) {
            tooltip.add(ClientTooltipComponent.create(Component.literal("§6" + Component.translatable("r3ct_daily.rewards.tooltip.streak.bonus_active").getString()).getVisualOrderText()));
            tooltip.add(ClientTooltipComponent.create(Component.literal("§7" + Component.translatable("r3ct_daily.rewards.tooltip.streak.bonus_desc").getString()).getVisualOrderText()));
        } else {
            tooltip.add(ClientTooltipComponent.create(Component.literal("§f" + Component.translatable("r3ct_daily.rewards.tooltip.streak.req1").getString()).getVisualOrderText()));
            tooltip.add(ClientTooltipComponent.create(Component.literal("§f" + Component.translatable("r3ct_daily.rewards.tooltip.streak.req2").getString()).getVisualOrderText()));
        }

        tooltip.add(ClientTooltipComponent.create(Component.literal("").getVisualOrderText()));
        tooltip.add(ClientTooltipComponent.create(Component.literal("§b" + Component.translatable("r3ct_daily.quests.tooltip.streak.freeze_title").getString()).getVisualOrderText()));
        tooltip.add(ClientTooltipComponent.create(Component.literal("").getVisualOrderText()));
        tooltip.add(ClientTooltipComponent.create(Component.literal("§f" + Component.translatable("r3ct_daily.rewards.tooltip.streak.freeze_desc1").getString()).getVisualOrderText()));
        tooltip.add(ClientTooltipComponent.create(Component.literal("§f" + Component.translatable("r3ct_daily.rewards.tooltip.streak.freeze_desc2").getString()).getVisualOrderText()));
        tooltip.add(ClientTooltipComponent.create(Component.literal("").getVisualOrderText()));
        int maxRewardShields = payload.maxRewardShields();
        tooltip.add(ClientTooltipComponent.create(Component.literal("§f" + Component.translatable("r3ct_daily.quests.tooltip.streak.freezes").getString() + " §b" + data.availableRewardFreezes + "§f/" + maxRewardShields).getVisualOrderText()));

        guiGraphics.tooltip(this.font, tooltip, mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null);
    }

    private void renderBonusTooltip(GuiGraphicsExtractor guiGraphics, int absoluteCollected, int mouseX, int mouseY) {
        List<ClientTooltipComponent> tooltip = new ArrayList<>();
        tooltip.add(ClientTooltipComponent.create(Component.literal("§d§l" + Component.translatable("r3ct_daily.rewards.tooltip.bonus_main.title").getString()).getVisualOrderText()));
        tooltip.add(ClientTooltipComponent.create(Component.literal("§8----------------").getVisualOrderText()));

        DailyServerConfig.MilestoneReward[] mr = {
                DailyServerConfig.bonuses.bonus_7,
                DailyServerConfig.bonuses.bonus_14,
                DailyServerConfig.bonuses.bonus_21
        };
        int[] thresholds = {7, 14, 21};

        for (int i = 0; i < 3; i++) {
            ItemStack stack = QuestManager.getMilestoneRewardStack(mr[i]);
            String label = mr[i].amount + "x " + stack.getHoverName().getString();
            tooltip.add(ClientTooltipComponent.create(
                    getBonusLine(absoluteCollected, thresholds[i], label, mr[i].getFormattedColor()).getVisualOrderText()
            ));
        }

        guiGraphics.tooltip(this.font, tooltip, mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null);
    }

    private Component getBonusLine(int absoluteCollected, int targetDay, String reward, String color) {
        int cycle = (absoluteCollected == 0) ? 0 : (absoluteCollected - 1) / 21;
        int absTarget = cycle * 21 + targetDay;
        boolean claimed = data.claimedBonusRewards.contains(absTarget);
        boolean canClaim = absoluteCollected >= absTarget && !claimed;

        String prefix = claimed ? "§a[ ✔ ] §a" : (canClaim ? "§e[ ! ] §e" : "§7[ ] §f");

        int visualCurrent = absoluteCollected - (cycle * 21);
        if (visualCurrent > targetDay) visualCurrent = targetDay;

        return Component.literal(prefix + Math.min(visualCurrent, targetDay) + "/" + targetDay + " " + Component.translatable("r3ct_daily.leaderboard.tooltip.days").getString() + " §8- " + color + reward);
    }

    private void renderSimpleTooltip(GuiGraphicsExtractor guiGraphics, String title, String info, int mouseX, int mouseY) {
        List<ClientTooltipComponent> tooltip = new ArrayList<>();
        tooltip.add(ClientTooltipComponent.create(Component.literal(title).getVisualOrderText()));
        tooltip.add(ClientTooltipComponent.create(Component.literal("§8----------------").getVisualOrderText()));
        tooltip.add(ClientTooltipComponent.create(Component.literal(info).getVisualOrderText()));
        guiGraphics.tooltip(this.font, tooltip, mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null);
    }

    private boolean isMouseOverSlot(double mouseX, double mouseY, int x, int y) {
        return mouseX >= x && mouseX <= x + 18 && mouseY >= y && mouseY <= y + 18;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
        if (event.button() == 0) {
            float scale = DailyClientConfig.getInstance().rewardScreenScale;

            int mX = (int)((event.x() - this.width / 2f) / scale + this.width / 2f);
            int mY = (int)((event.y() - this.height / 2f) / scale + this.height / 2f);

            int leftPos = (this.width - imageWidth) / 2;
            int topPos = (this.height - imageHeight) / 2;

            int trophyX = leftPos + imageWidth - 24;
            int trophyY = topPos + 6;

            if (mX >= trophyX && mX <= trophyX + 16 && mY >= trophyY && mY <= trophyY + 16) {
                if (this.minecraft != null && this.minecraft.player != null) {
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    Services.PLATFORM.sendToServer(new RequestLeaderboardPayload(1));
                    return true;
                }
            }

            String arrowText = Component.translatable("r3ct_daily.leaderboard.button.quests_prev").getString();
            int textWidth = this.font.width(arrowText);
            int arrowX = leftPos - 15 - textWidth;
            int arrowY = (this.height / 2) - 4;

            if (mX >= arrowX && mX <= arrowX + textWidth && mY >= arrowY - 2 && mY <= arrowY + 10) {
                if (this.minecraft != null && this.minecraft.player != null) {
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
                    this.minecraft.player.connection.sendCommand("daily quests");
                    return true;
                }
            }

            String dzisiajData = getCurrentQuestDateString();
            boolean juzDzisiajOdebrane = dzisiajData.equals(data.lastRewardDate);

            for (int i = 1; i <= 7; i++) {
                int slotX = leftPos + 27 + (i - 1) * 43;
                int slotY = topPos + 43;

                if (isMouseOverSlot(mX, mY, slotX, slotY) && i == data.rewardDay && !juzDzisiajOdebrane) {
                    if (Minecraft.getInstance().player != null) {
                        Minecraft.getInstance().player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
                        Minecraft.getInstance().player.connection.sendCommand("daily claimreward");
                        return true;
                    }
                }
            }

            int barY = topPos + 140;
            int bar2X = leftPos + 173;
            int bWidth = 135;
            int[] thresholds = {7, 14, 21};
            DailyServerConfig.MilestoneReward[] mr = {
                    DailyServerConfig.bonuses.bonus_7,
                    DailyServerConfig.bonuses.bonus_14,
                    DailyServerConfig.bonuses.bonus_21
            };
            int cycle = (data.totalCollected == 0) ? 0 : (data.totalCollected - 1) / 21;
            for (int i = 0; i < 3; i++) {
                int t = thresholds[i];
                String amountsStr = mr[i].getFormattedColor() + "x" + mr[i].amount;
                int mX_bonus = (t == 21) ? (bar2X + bWidth - 1) : (bar2X + (int)(t * (bWidth / 21.0)));
                int totalW = 18 + this.font.width(amountsStr);
                int startX = mX_bonus - totalW / 2;
                int startY = barY + 24;

                if (mX >= startX - 2 && mX <= startX + totalW + 2 && mY >= startY - 2 && mY <= startY + 18) {
                    int absTarget = cycle * 21 + t;
                    boolean canClaim = data.totalCollected >= absTarget && !data.claimedBonusRewards.contains(absTarget);

                    if (canClaim) {
                        if (this.minecraft != null && this.minecraft.player != null) {
                            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                            this.minecraft.player.connection.sendCommand("daily claimbonus " + t);
                            return true;
                        }
                    }
                }
            }

            int midX = leftPos + (imageWidth / 2);
            String backText = Component.translatable("r3ct_daily.quests.button.close").getString();
            int backWidth = this.font.width(backText);
            int backX = midX - (backWidth / 2);
            int backY = topPos + imageHeight + 8;

            if (mX >= backX - 2 && mX <= backX + backWidth + 2 && mY >= backY - 2 && mY <= backY + 10) {
                if (this.minecraft != null && this.minecraft.player != null) {
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    this.onClose();
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (Services.PLATFORM.isRewardKey(event)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    private ItemStack getIconForDay(int day) {
        if (day <= 4) return new ItemStack(Items.BARREL);
        if (day <= 6) return new ItemStack(Items.CHEST);
        return new ItemStack(Items.ENDER_CHEST);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}