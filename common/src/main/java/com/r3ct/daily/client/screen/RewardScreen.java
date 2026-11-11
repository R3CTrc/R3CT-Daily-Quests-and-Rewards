package com.r3ct.daily.client.screen;

import com.r3ct.daily.config.DailyClientConfig;
import com.r3ct.daily.config.DailyServerConfig;
import com.r3ct.daily.logic.QuestManager;
import com.r3ct.daily.logic.RewardManager;
import com.r3ct.daily.network.OpenRewardsPayload;
import com.r3ct.daily.platform.Services;
import com.r3ct.daily.data.PlayerData;
import com.r3ct.daily.network.RequestLeaderboardPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RewardScreen extends Screen {
    private final PlayerData data;
    private final OpenRewardsPayload payload;

    private final int imageWidth = 344;
    private final int imageHeight = 200;

    private static final int COMPARTMENT_WIDTH = 46;
    private static final int BAY_OFFSET_X = 11;

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

    private int getSlotX(int leftPos, int dayIndex) {
        int compartmentLeft = leftPos + BAY_OFFSET_X + (dayIndex - 1) * COMPARTMENT_WIDTH;
        return compartmentLeft + (COMPARTMENT_WIDTH / 2) - 9;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        float scale = DailyClientConfig.getInstance().rewardScreenScale;

        mouseX = (int)((mouseX - this.width / 2f) / scale + this.width / 2f);
        mouseY = (int)((mouseY - this.height / 2f) / scale + this.height / 2f);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(this.width / 2f, this.height / 2f, 0f);
        guiGraphics.pose().scale(scale, scale, 1f);
        guiGraphics.pose().translate(-this.width / 2f, -this.height / 2f, 0f);

        int leftPos = (this.width - imageWidth) / 2;
        int topPos = (this.height - imageHeight) / 2;
        int midX = leftPos + (imageWidth / 2);
        long time = System.currentTimeMillis();

        drawTreasureChestCoffer(guiGraphics, leftPos, topPos);

        String headerText = "§l" + Component.translatable("r3ct_daily.rewards.header.rewards").getString();
        int headerW = this.font.width(headerText);
        int plaqueLeft = midX - (headerW / 2) - 12;
        int plaqueRight = midX + (headerW / 2) + 12;
        guiGraphics.fill(plaqueLeft, topPos + 7, plaqueRight, topPos + 23, 0xFF2B170C);
        guiGraphics.fill(plaqueLeft + 1, topPos + 8, plaqueRight - 1, topPos + 22, 0xFFD4AF37);
        guiGraphics.fill(plaqueLeft + 2, topPos + 9, plaqueRight - 2, topPos + 21, 0xFFF7E4BE);
        guiGraphics.drawString(this.font, headerText, midX - (headerW / 2), topPos + 11, 0xFF3E2723, false);

        int hoveredDay = -1;
        String dzisiajData = getCurrentQuestDateString();
        boolean juzDzisiajOdebrane = dzisiajData.equals(data.lastRewardDate);

        for (int i = 1; i <= 7; i++) {
            int slotX = getSlotX(leftPos, i);
            int slotY = topPos + 43;

            boolean czyOdebrane = juzDzisiajOdebrane ? ((data.rewardDay == 1) || (i < data.rewardDay)) : (i < data.rewardDay);
            boolean czyMoznaOdebrac = (i == data.rewardDay) && !juzDzisiajOdebrane;
            boolean slotHovered = isMouseOverSlot(mouseX, mouseY, slotX, slotY);

            String dayTxt = Component.translatable("r3ct_daily.rewards.day", i).getString();
            int dayTextWidth = this.font.width(dayTxt);

            drawDayPedestalSlot(guiGraphics, slotX, slotY, i, dayTextWidth, czyOdebrane, czyMoznaOdebrac, slotHovered, time);

            if (slotHovered) {
                hoveredDay = i;
            }

            ItemStack icon = getIconForDay(i);
            guiGraphics.renderItem(icon, slotX + 1, slotY + 1);
            guiGraphics.renderItemDecorations(this.font, icon, slotX + 1, slotY + 1);

            if (czyMoznaOdebrac && data.streak >= 7) {
                guiGraphics.drawString(this.font, "§6§l+", slotX + 12, slotY + 12, 0xFFFFFFFF, true);
            }

            int kolorTekstu;
            if (czyOdebrane) {
                kolorTekstu = 0xFF887A7A;
            } else {
                if (i <= 4) kolorTekstu = 0xFFFFFF55;
                else if (i <= 6) kolorTekstu = 0xFF55FFFF;
                else kolorTekstu = 0xFFFF55FF;
            }

            guiGraphics.drawString(this.font, dayTxt, (slotX + 9) - (dayTextWidth / 2), slotY + 24, kolorTekstu, true);

            if (czyMoznaOdebrac) {
                String arrow = "▲";
                int arrowColor = (time % 1000 < 500) ? 0xFF55FF55 : 0xFFFFAA00;
                guiGraphics.drawString(this.font, arrow, (slotX + 9) - (this.font.width(arrow) / 2), slotY + 37, arrowColor, true);
            }

            if (czyOdebrane) {
                guiGraphics.drawString(this.font, "§a✔", slotX + 11, slotY + 11, 0xFFFFFFFF, true);
            }
        }

        int barY = topPos + 140;
        int bWidth = 135;
        int bar1X = leftPos + 21;
        int bar2X = midX + 16;

        int targetStreak = Math.min(data.streak, 7);
        animatedStreak += (targetStreak - animatedStreak) * 0.1f;
        if (Math.abs(targetStreak - animatedStreak) < 0.05f) animatedStreak = targetStreak;

        String streakColorCode = (targetStreak < 3) ? "§2" : (targetStreak < 7 ? "§6" : "§c");
        int sColor = (targetStreak < 3) ? 0xFF006400 : (targetStreak < 7 ? 0xFFFFAA00 : 0xFFFF5555);

        String lblStreak = Component.translatable("r3ct_daily.rewards.bar.streak").getString();
        String valStreak = streakColorCode + data.absoluteRewardStreak + "§0/7";
        GuiUtils.drawRewardStyleBar(guiGraphics, this.font, bar1X, barY, animatedStreak, 7, lblStreak, valStreak, sColor, bWidth, 1, new int[]{});

        String multiText = "§8" + Component.translatable("r3ct_daily.gui.streak_bonus").getString() + (data.streak >= 7 ? "§a" + Component.translatable("r3ct_daily.gui.active").getString() : Component.translatable("r3ct_daily.gui.inactive").getString());
        guiGraphics.drawString(this.font, multiText, bar1X, barY + 14, 0xFFFFFFFF, false);

        int maxRewardShields = payload.maxRewardShields();
        String shieldsText = "§8" + Component.translatable("r3ct_daily.gui.available_shields").getString() + " §b" + data.availableRewardFreezes + "§8/" + maxRewardShields;
        guiGraphics.drawString(this.font, shieldsText, bar1X, barY + 26, 0xFFFFFFFF, false);

        int activeCycle = RewardManager.getActiveBonusCycle(data);
        int displayCollected = Math.min(21, Math.max(0, data.totalCollected - (activeCycle * 21)));

        animatedCollected += (displayCollected - animatedCollected) * 0.1f;
        if (Math.abs(displayCollected - animatedCollected) < 0.05f) animatedCollected = displayCollected;

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

        guiGraphics.drawString(this.font, arrowText, arrowX, arrowY, color, true);

        int bayRightEdge = leftPos + imageWidth - BAY_OFFSET_X + 1;
        int trophyX = bayRightEdge - 18;
        int trophyY = topPos + 7;
        boolean trophyHover = mouseX >= trophyX - 2 && mouseX <= trophyX + 18 && mouseY >= trophyY - 1 && mouseY <= trophyY + 17;

        guiGraphics.fill(trophyX - 2, trophyY - 1, trophyX + 18, trophyY + 17, 0xFF2B170C);
        guiGraphics.fill(trophyX - 1, trophyY, trophyX + 17, trophyY + 16, trophyHover ? 0xFFFFE066 : 0xFFD4AF37);
        guiGraphics.fill(trophyX, trophyY + 1, trophyX + 16, trophyY + 15, 0xFF4A2E1B);
        if (trophyHover) guiGraphics.fill(trophyX, trophyY + 1, trophyX + 16, trophyY + 15, 0x44FFFFFF);
        guiGraphics.renderItem(new ItemStack(Items.MOJANG_BANNER_PATTERN), trophyX, trophyY);

        String backText = Component.translatable("r3ct_daily.quests.button.close").getString();
        int backWidth = this.font.width(backText);
        int backX = midX - (backWidth / 2);
        int backY = topPos + imageHeight + 8;

        boolean backHover = mouseX >= backX - 2 && mouseX <= backX + backWidth + 2 && mouseY >= backY - 2 && mouseY <= backY + 10;
        int backColor = backHover ? 0xFFFF5555 : 0xFFAAAAAA;
        guiGraphics.drawString(this.font, backText, backX, backY, backColor, true);

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

        for (int i = 0; i < 3; i++) {
            String rewardColor = mr[i].getFormattedColor();

            titles[i] = rewardColor + "§l" + Component.translatable("r3ct_daily.rewards.tooltip.bonus.title", thresholds[i]).getString();
            ItemStack stack = QuestManager.getMilestoneRewardStack(mr[i]);
            rewards[i] = "§f" + rewLabel + " " + rewardColor + mr[i].amount + "x " + stack.getHoverName().getString();
        }

        for (int i = 0; i < 3; i++) {
            int t = thresholds[i];
            int mX = (t == 21) ? (bar2X + bWidth - 1) : (bar2X + (int)(t * (bWidth / 21.0)));
            int startY = barY + 24;

            if (mouseX >= mX - 11 && mouseX <= mX + 11 && mouseY >= startY - 3 && mouseY <= startY + 19) {
                renderSimpleTooltip(guiGraphics, titles[i], rewards[i], mouseX, mouseY);
            }
        }

        if (trophyHover) {
            List<Component> tTooltip = new ArrayList<>();
            tTooltip.add(Component.literal("§6§l" + Component.translatable("r3ct_daily.quests.tooltip.leaderboard.title").getString()));
            tTooltip.add(Component.literal("§8----------------"));
            tTooltip.add(Component.literal("§f" + Component.translatable("r3ct_daily.quests.tooltip.leaderboard.desc").getString()));
            guiGraphics.renderComponentTooltip(this.font, tTooltip, mouseX, mouseY);
        }

        guiGraphics.pose().popPose();
    }

    private void drawTreasureChestCoffer(GuiGraphics g, int leftPos, int topPos) {
        int rightPos = leftPos + imageWidth;
        int bottomPos = topPos + imageHeight;
        int midX = leftPos + (imageWidth / 2);

        g.fill(leftPos - 3, topPos - 3, rightPos + 3, bottomPos + 3, 0xFF1A0E07);
        g.fill(leftPos - 2, topPos - 2, rightPos + 2, bottomPos + 2, 0xFF4A2816);

        g.fill(leftPos + 2, topPos + 2, rightPos - 2, bottomPos - 2, 0xFF9E7B22);
        g.fill(leftPos + 3, topPos + 3, rightPos - 3, bottomPos - 3, 0xFF3B2012);

        int bayLeft = leftPos + BAY_OFFSET_X;
        int bayTop = topPos + 28;
        int bayRight = rightPos - BAY_OFFSET_X;
        int bayBottom = topPos + 92;

        g.fill(bayLeft - 1, bayTop - 1, bayRight + 1, bayBottom + 1, 0xFFD4AF37);
        g.fill(bayLeft, bayTop, bayRight, bayBottom, 0xFF2D1219);

        for (int i = 1; i < 7; i++) {
            int seamX = bayLeft + i * COMPARTMENT_WIDTH;
            g.fill(seamX - 1, bayTop + 2, seamX, bayBottom - 2, 0xFF210B10);
            g.fill(seamX, bayTop + 2, seamX + 1, bayBottom - 2, 0xFF3B1821);
        }

        g.fill(bayLeft, bayTop, bayRight, bayTop + 3, 0x77000000);
        g.fill(bayLeft, bayTop, bayLeft + 3, bayBottom, 0x55000000);
        g.fill(bayLeft, bayBottom - 2, bayRight, bayBottom, 0x44000000);
        g.fill(bayRight - 2, bayTop, bayRight, bayBottom, 0x44000000);

        int beamY = topPos + 95;
        g.fill(leftPos - 2, beamY, rightPos + 2, beamY + 7, 0xFF241209);
        g.fill(leftPos, beamY + 1, rightPos, beamY + 6, 0xFF5A321C);
        g.fill(leftPos, beamY + 1, rightPos, beamY + 2, 0xFFB8860B);
        g.fill(leftPos, beamY + 5, rightPos, beamY + 6, 0xFF8C6D1F);

        int lowerLeft = leftPos + 6;
        int lowerTop = topPos + 104;
        int lowerRight = rightPos - 6;
        int lowerBottom = bottomPos - 5;

        g.fill(lowerLeft - 1, lowerTop - 1, lowerRight + 1, lowerBottom + 1, 0xFFA68B65);
        g.fill(lowerLeft, lowerTop, lowerRight, lowerBottom, 0xFFF2E0C4);
        g.fill(lowerLeft + 2, lowerTop + 2, lowerRight - 2, lowerBottom - 2, 0xFFF7E9D2);

        g.fill(midX - 1, lowerTop + 4, midX, lowerBottom - 4, 0xFFB89B72);
        g.fill(midX, lowerTop + 4, midX + 1, lowerBottom - 4, 0xFFFFF5E4);

        g.fill(midX - 11, beamY - 3, midX + 11, beamY + 11, 0x55000000);
        g.fill(midX - 10, beamY - 4, midX + 10, beamY + 10, 0xFF7A5914);
        g.fill(midX - 9, beamY - 3, midX + 9, beamY + 9, 0xFFD4AF37);
        g.fill(midX - 8, beamY - 2, midX + 8, beamY - 1, 0xFFFFE885);

        g.fill(midX - 2, beamY + 1, midX + 2, beamY + 4, 0xFF241209);
        g.fill(midX - 1, beamY + 4, midX + 1, beamY + 7, 0xFF241209);

        drawGoldCorner(g, leftPos - 3, topPos - 3, true, true);
        drawGoldCorner(g, rightPos - 7, topPos - 3, false, true);
        drawGoldCorner(g, leftPos - 3, bottomPos - 7, true, false);
        drawGoldCorner(g, rightPos - 7, bottomPos - 7, false, false);
    }

    private void drawGoldCorner(GuiGraphics g, int x, int y, boolean left, boolean top) {
        int goldDark = 0xFF8C6D1F;
        int goldBright = 0xFFE6C259;
        int rivet = 0xFFFFF2B2;

        int hY = top ? y : y + 6;
        g.fill(x, hY, x + 10, hY + 4, goldDark);
        g.fill(x + 1, hY + 1, x + 9, hY + 2, goldBright);

        int vX = left ? x : x + 6;
        g.fill(vX, y, vX + 4, y + 10, goldDark);
        g.fill(vX + 1, y + 1, vX + 2, y + 9, goldBright);

        g.fill(vX + 1, hY + 1, vX + 3, hY + 3, rivet);
    }

    private void drawDayPedestalSlot(GuiGraphics g, int slotX, int slotY, int day, int textWidth, boolean czyOdebrane, boolean czyMoznaOdebrac, boolean isHovered, long time) {
        int centerX = slotX + 9;
        int plaqueHalfW = Math.max(20, (textWidth / 2) + 4);
        int plaqueLeft = centerX - plaqueHalfW;
        int plaqueRight = centerX + plaqueHalfW;

        g.fill(slotX - 2, slotY - 1, slotX + 20, slotY + 20, 0x44000000);
        g.fill(plaqueLeft + 1, slotY + 22, plaqueRight + 1, slotY + 36, 0x44000000);

        int trimColor;
        if (czyOdebrane) {
            trimColor = 0xFF4A3B3E;
        } else if (day <= 4) {
            trimColor = 0xFFA87948;
        } else if (day <= 6) {
            trimColor = 0xFF48A0B8;
        } else {
            trimColor = 0xFFC85AD4;
        }

        if (czyMoznaOdebrac) {
            int pulseAlpha = (int) (130 + 80 * Math.sin(time / 160.0));
            int glow = (pulseAlpha << 24) | 0xFFAA00;
            g.fill(slotX - 3, slotY - 3, slotX + 21, slotY + 21, glow);
            trimColor = 0xFFFFD700;
        }

        g.fill(plaqueLeft, slotY + 21, plaqueRight, slotY + 35, 0xFF1A090D);
        g.fill(plaqueLeft + 1, slotY + 22, plaqueRight - 1, slotY + 34, czyOdebrane ? 0xFF2B1A1E : 0xFF3E1923);
        g.fill(plaqueLeft + 1, slotY + 22, plaqueRight - 1, slotY + 23, trimColor);

        g.fill(slotX - 1, slotY - 1, slotX + 19, slotY + 19, 0xFF1A090D);
        g.fill(slotX, slotY, slotX + 18, slotY + 18, trimColor);

        int innerColor = czyOdebrane ? 0xFF1F1719 : (czyMoznaOdebrac ? 0xFF4A3210 : 0xFF26121A);
        g.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, innerColor);

        g.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 2, 0x55000000);
        g.fill(slotX + 1, slotY + 1, slotX + 2, slotY + 17, 0x55000000);

        if (isHovered) {
            g.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, 0x55FFFFFF);
        }
    }

    private void renderBonusMilestones(GuiGraphics g, int x, int y, int bWidth, int mouseX, int mouseY) {
        int[] thresholds = {7, 14, 21};
        DailyServerConfig.MilestoneReward[] mr = {
                DailyServerConfig.bonuses.bonus_7,
                DailyServerConfig.bonuses.bonus_14,
                DailyServerConfig.bonuses.bonus_21
        };

        ItemStack[] icons = new ItemStack[3];
        for (int i = 0; i < 3; i++) {
            icons[i] = QuestManager.getMilestoneRewardStack(mr[i]);
        }

        long time = System.currentTimeMillis();
        int cycle = RewardManager.getActiveBonusCycle(data);

        for (int i = 0; i < 3; i++) {
            int t = thresholds[i];
            int absTarget = cycle * 21 + t;
            boolean canClaim = data.totalCollected >= absTarget && !data.claimedBonusRewards.contains(absTarget);
            boolean claimed = data.claimedBonusRewards.contains(absTarget);

            int mX = (t == 21) ? (x + bWidth - 1) : (x + (int)(t * (bWidth / 21.0)));

            g.fill(mX, y - 2, mX + 1, y + 10, 0xFFFFFFFF);
            String txtDay = "§8" + t;
            g.drawString(this.font, txtDay, mX - (this.font.width(String.valueOf(t)) / 2), y + 12, 0xFFFFFFFF, false);

            int startY = y + 24;
            int boxLeft = mX - 11;
            int boxRight = mX + 11;

            boolean hovered = mouseX >= boxLeft && mouseX <= boxRight && mouseY >= startY - 3 && mouseY <= startY + 19;

            if (claimed) {
                g.fill(boxLeft, startY - 3, boxRight, startY + 19, 0x33000000);
            } else if (canClaim) {
                int color = hovered ? 0x66FFFF00 : 0x44FFFF00;
                g.fill(boxLeft, startY - 3, boxRight, startY + 19, color);
            } else if (hovered) {
                g.fill(boxLeft, startY - 3, boxRight, startY + 19, 0x22000000);
            }

            drawStackedItemPile(g, icons[i], mX, startY, mr[i].amount);

            if (claimed) {
                g.pose().pushPose();
                g.pose().translate(0, 0, 0f);
                g.drawString(this.font, "§a✔", mX + 3, startY + 9, 0xFFFFFFFF, true);
                g.pose().popPose();
            } else if (canClaim) {
                g.pose().pushPose();
                g.pose().translate(mX, startY + 21, 0f);
                g.pose().scale(1.5f, 1.5f, 1f);
                String arrow = (time % 1000 < 500) ? "§e↑" : "§6↑";
                g.drawString(this.font, arrow, -this.font.width("↑") / 2, 0, 0xFF000000, true);
                g.pose().popPose();
            }
        }
    }

    private int getVisualPileCount(int amount) {
        if (amount <= 1) return 1;
        if (amount <= 8) return 2;
        if (amount <= 16) return 3;
        return 4;
    }

    private void drawStackedItemPile(GuiGraphics g, ItemStack stack, int centerX, int y, int amount) {
        int count = getVisualPileCount(amount);
        int baseX = centerX - 8;

        if (count == 1) {
            g.renderItem(stack, baseX, y);
        } else if (count == 2) {
            g.renderItem(stack, baseX - 3, y - 2);
            g.renderItem(stack, baseX + 3, y + 1);
        } else if (count == 3) {
            g.renderItem(stack, baseX - 4, y - 2);
            g.renderItem(stack, baseX + 4, y - 2);
            g.renderItem(stack, baseX, y + 2);
        } else {
            g.renderItem(stack, baseX, y - 4);
            g.renderItem(stack, baseX - 5, y - 1);
            g.renderItem(stack, baseX + 5, y - 1);
            g.renderItem(stack, baseX, y + 2);
        }
    }

    private void renderDayTooltip(GuiGraphics guiGraphics, int day, ItemStack stack, int mouseX, int mouseY) {
        String dzisiajData = getCurrentQuestDateString();
        boolean juzDzisiajOdebrane = dzisiajData.equals(data.lastRewardDate);
        boolean czyOdebrane = juzDzisiajOdebrane ? ((data.rewardDay == 1) || (day < data.rewardDay)) : (day < data.rewardDay);
        boolean czyMoznaOdebrac = (day == data.rewardDay) && !juzDzisiajOdebrane;

        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.literal("§6§l" + Component.translatable("r3ct_daily.rewards.tooltip.day.title", day).getString()));

        String statusRaw = czyOdebrane ? "§a" + Component.translatable("r3ct_daily.rewards.status.claimed").getString() : (czyMoznaOdebrac ? "§e" + Component.translatable("r3ct_daily.rewards.status.claimable").getString() : "§c" + Component.translatable("r3ct_daily.rewards.status.locked").getString());
        tooltip.add(Component.literal(Component.translatable("r3ct_daily.rewards.tooltip.status").getString() + " " + statusRaw));

        tooltip.add(Component.literal("§8----------------"));

        if (czyOdebrane) {
            String zHisto = (data.claimedRewardHistory != null && data.claimedRewardHistory.size() > day - 1) ? data.claimedRewardHistory.get(day - 1) : "";
            if (zHisto != null && !zHisto.isEmpty()) {
                tooltip.add(Component.literal("§f" + Component.translatable("r3ct_daily.rewards.tooltip.content").getString()));

                String[] items = zHisto.split(",");
                for (String itemStr : items) {
                    itemStr = itemStr.trim();

                    if (itemStr.contains(";")) {
                        String[] parts = itemStr.split(";", 2);
                        String amount = parts[0];
                        String translationKey = parts[1];

                        MutableComponent line = Component.literal("  - §b" + amount + "x ").append(Component.translatable(translationKey).withStyle(ChatFormatting.AQUA));
                        tooltip.add(line);
                    } else {
                        tooltip.add(Component.literal("  - " + itemStr));
                    }
                }
            } else {
                tooltip.add(Component.literal("§f" + Component.translatable("r3ct_daily.rewards.tooltip.content").getString() + " §8(" + Component.translatable("r3ct_daily.rewards.tooltip.no_data").getString() + ")"));
            }
        } else if (day == 7) {
            tooltip.add(Component.literal("§f" + Component.translatable("r3ct_daily.rewards.tooltip.content").getString() + " §d" + Component.translatable("r3ct_daily.rewards.tooltip.epic_rewards").getString()));
            tooltip.add(Component.literal("§b+ 1 " + Component.translatable("r3ct_daily.rewards.tooltip.shield").getString()));
        } else {
            tooltip.add(Component.literal("§f" + Component.translatable("r3ct_daily.rewards.tooltip.content").getString() + " §7§k???"));
        }

        if (czyMoznaOdebrac && data.streak >= 7) {
            tooltip.add(Component.literal("§6★ " + Component.translatable("r3ct_daily.rewards.tooltip.bonus_loot").getString()));
        }

        guiGraphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
    }

    private void renderStreakTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.literal("§6§l" + Component.translatable("r3ct_daily.rewards.tooltip.streak.title").getString()));
        tooltip.add(Component.literal("§8----------------"));
        tooltip.add(Component.literal("§f" + Component.translatable("r3ct_daily.rewards.tooltip.streak.req1").getString()));
        tooltip.add(Component.literal("§f" + Component.translatable("r3ct_daily.rewards.tooltip.streak.req2").getString()));

        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§b" + Component.translatable("r3ct_daily.gui.streak_freeze_title").getString()));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("§f" + Component.translatable("r3ct_daily.rewards.tooltip.streak.freeze_desc1").getString()));
        tooltip.add(Component.literal("§f" + Component.translatable("r3ct_daily.rewards.tooltip.streak.freeze_desc2").getString()));
        tooltip.add(Component.literal(""));
        int maxRewardShields = payload.maxRewardShields();
        tooltip.add(Component.literal("§f" + Component.translatable("r3ct_daily.gui.available_shields").getString() + " §b" + data.availableRewardFreezes + "§f/" + maxRewardShields));

        guiGraphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
    }

    private void renderBonusTooltip(GuiGraphics guiGraphics, int absoluteCollected, int mouseX, int mouseY) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.literal("§d§l" + Component.translatable("r3ct_daily.rewards.tooltip.bonus_main.title").getString()));
        tooltip.add(Component.literal("§8----------------"));

        DailyServerConfig.MilestoneReward[] mr = {
                DailyServerConfig.bonuses.bonus_7,
                DailyServerConfig.bonuses.bonus_14,
                DailyServerConfig.bonuses.bonus_21
        };
        int[] thresholds = {7, 14, 21};

        for (int i = 0; i < 3; i++) {
            ItemStack stack = QuestManager.getMilestoneRewardStack(mr[i]);
            String label = mr[i].amount + "x " + stack.getHoverName().getString();
            tooltip.add(getBonusLine(absoluteCollected, thresholds[i], label, mr[i].getFormattedColor()));
        }

        guiGraphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
    }

    private Component getBonusLine(int absoluteCollected, int targetDay, String reward, String color) {
        int cycle = RewardManager.getActiveBonusCycle(data);
        int absTarget = cycle * 21 + targetDay;
        boolean claimed = data.claimedBonusRewards.contains(absTarget);
        boolean canClaim = absoluteCollected >= absTarget && !claimed;

        String prefix = claimed ? "§a[ ✔ ] §a" : (canClaim ? "§e[ ! ] §e" : "§7[ ] §f");

        int visualCurrent = Math.max(0, absoluteCollected - (cycle * 21));
        if (visualCurrent > targetDay) visualCurrent = targetDay;

        return Component.literal(prefix + Math.min(visualCurrent, targetDay) + "/" + targetDay + " " + Component.translatable("r3ct_daily.leaderboard.tooltip.days").getString() + " §8- " + color + reward);
    }

    private void renderSimpleTooltip(GuiGraphics guiGraphics, String title, String info, int mouseX, int mouseY) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.literal(title));
        tooltip.add(Component.literal("§8----------------"));
        tooltip.add(Component.literal(info));
        guiGraphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
    }

    private boolean isMouseOverSlot(double mouseX, double mouseY, int x, int y) {
        return mouseX >= x && mouseX <= x + 18 && mouseY >= y && mouseY <= y + 18;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            float scale = DailyClientConfig.getInstance().rewardScreenScale;

            int mX = (int)((mouseX - this.width / 2f) / scale + this.width / 2f);
            int mY = (int)((mouseY - this.height / 2f) / scale + this.height / 2f);

            int leftPos = (this.width - imageWidth) / 2;
            int topPos = (this.height - imageHeight) / 2;
            int midX = leftPos + (imageWidth / 2);

            int bayRightEdge = leftPos + imageWidth - BAY_OFFSET_X + 1;
            int trophyX = bayRightEdge - 18;
            int trophyY = topPos + 7;

            if (mX >= trophyX - 2 && mX <= trophyX + 18 && mY >= trophyY - 1 && mY <= trophyY + 17) {
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
                int slotX = getSlotX(leftPos, i);
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
            int bar2X = midX + 16;
            int bWidth = 135;
            int[] thresholds = {7, 14, 21};
            int cycle = RewardManager.getActiveBonusCycle(data);

            for (int i = 0; i < 3; i++) {
                int t = thresholds[i];
                int mX_bonus = (t == 21) ? (bar2X + bWidth - 1) : (bar2X + (int)(t * (bWidth / 21.0)));
                int startY = barY + 24;

                if (mX >= mX_bonus - 11 && mX <= mX_bonus + 11 && mY >= startY - 3 && mY <= startY + 19) {
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
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (Services.PLATFORM.isRewardKey(keyCode)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private ItemStack getIconForDay(int day) {
        if (day <= 4) return new ItemStack(Items.BARREL);
        if (day <= 6) return new ItemStack(Items.CHEST);
        return new ItemStack(Items.ENDER_CHEST);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}