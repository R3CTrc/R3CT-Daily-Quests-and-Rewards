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
import org.lwjgl.sdl.SDLMouse;

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
        guiGraphics.text(this.font, headerText, midX - (headerW / 2), topPos + 11, 0xFF3E2723, false);

        int hoveredDay = -1;
        String todayDate = getCurrentQuestDateString();
        boolean alreadyClaimedToday = todayDate.equals(data.lastRewardDate);

        for (int i = 1; i <= 7; i++) {
            int slotX = getSlotX(leftPos, i);
            int slotY = topPos + 43;

            boolean isClaimed = alreadyClaimedToday ? ((data.rewardDay == 1) || (i < data.rewardDay)) : (i < data.rewardDay);
            boolean canClaim = (i == data.rewardDay) && !alreadyClaimedToday;
            boolean slotHovered = isMouseOverSlot(mouseX, mouseY, slotX, slotY);

            String dayTxt = Component.translatable("r3ct_daily.rewards.day", i).getString();
            int dayTextWidth = this.font.width(dayTxt);

            drawDayPedestalSlot(guiGraphics, slotX, slotY, i, dayTextWidth, isClaimed, canClaim, slotHovered, time);

            if (slotHovered) {
                hoveredDay = i;
            }

            ItemStack icon = getIconForDay(i);
            guiGraphics.item(icon, slotX + 1, slotY + 1);
            guiGraphics.itemDecorations(this.font, icon, slotX + 1, slotY + 1);

            if (canClaim && data.rewardStreak >= 7) {
                guiGraphics.text(this.font, "§6§l+", slotX + 12, slotY + 12, 0xFFFFFFFF, true);
            }

            int textColor;
            if (isClaimed) {
                textColor = 0xFF887A7A;
            } else {
                if (i <= 4) textColor = 0xFFFFFF55;
                else if (i <= 6) textColor = 0xFF55FFFF;
                else textColor = 0xFFFF55FF;
            }

            guiGraphics.text(this.font, dayTxt, (slotX + 9) - (dayTextWidth / 2), slotY + 24, textColor, true);

            if (canClaim) {
                String arrow = "▲";
                int arrowColor = (time % 1000 < 500) ? 0xFF55FF55 : 0xFFFFAA00;
                guiGraphics.text(this.font, arrow, (slotX + 9) - (this.font.width(arrow) / 2), slotY + 37, arrowColor, true);
            }

            if (isClaimed) {
                guiGraphics.text(this.font, "§a✔", slotX + 11, slotY + 11, 0xFFFFFFFF, true);
            }
        }

        int barY = topPos + 140;
        int barWidth = 135;
        int bar1X = leftPos + 21;
        int bar2X = midX + 16;

        int targetStreak = Math.min(data.rewardStreak, 7);
        animatedStreak += (targetStreak - animatedStreak) * 0.1f;
        if (Math.abs(targetStreak - animatedStreak) < 0.05f) animatedStreak = targetStreak;

        String streakColorCode = (targetStreak < 3) ? "§2" : (targetStreak < 7 ? "§6" : "§c");
        int streakColor = (targetStreak < 3) ? 0xFF006400 : (targetStreak < 7 ? 0xFFFFAA00 : 0xFFFF5555);

        String lblStreak = Component.translatable("r3ct_daily.rewards.bar.streak").getString();
        String valStreak = streakColorCode + data.absoluteRewardStreak + "§0/7";
        GuiUtils.drawRewardStyleBar(guiGraphics, this.font, bar1X, barY, animatedStreak, 7, lblStreak, valStreak, streakColor, barWidth, 1, new int[]{});

        String multiText = "§8" + Component.translatable("r3ct_daily.gui.streak_bonus").getString() + (data.rewardStreak >= 7 ? "§a" + Component.translatable("r3ct_daily.gui.active").getString() : Component.translatable("r3ct_daily.gui.inactive").getString());
        guiGraphics.text(this.font, multiText, bar1X, barY + 14, 0xFFFFFFFF, false);

        int maxRewardShields = payload.maxRewardShields();
        String shieldsText = "§8" + Component.translatable("r3ct_daily.gui.available_shields").getString() + " §b" + data.availableRewardShields + "§8/" + maxRewardShields;
        guiGraphics.text(this.font, shieldsText, bar1X, barY + 26, 0xFFFFFFFF, false);

        int activeCycle = RewardManager.getActiveBonusCycle(data);
        int displayCollected = Math.min(21, Math.max(0, data.totalCollected - (activeCycle * 21)));

        animatedCollected += (displayCollected - animatedCollected) * 0.1f;
        if (Math.abs(displayCollected - animatedCollected) < 0.05f) animatedCollected = displayCollected;

        String lblBonus = Component.translatable("r3ct_daily.rewards.bar.bonus").getString();
        String valBonus = "§d" + displayCollected + "§0/21";
        GuiUtils.drawRewardStyleBar(guiGraphics, this.font, bar2X, barY, animatedCollected, 21, lblBonus, valBonus, 0xFFFF55FF, barWidth, 1, new int[]{});

        renderBonusMilestones(guiGraphics, bar2X, barY, barWidth, mouseX, mouseY);

        String arrowText = Component.translatable("r3ct_daily.leaderboard.button.quests_prev").getString();
        int textWidth = this.font.width(arrowText);
        int arrowX = leftPos - 15 - textWidth;
        int arrowY = (this.height / 2) - 4;

        boolean isHovered = mouseX >= arrowX && mouseX <= arrowX + textWidth && mouseY >= arrowY - 2 && mouseY <= arrowY + 10;
        int color = isHovered ? 0xFFFFFFFF : 0xFFAAAAAA;

        guiGraphics.text(this.font, arrowText, arrowX, arrowY, color, true);

        int bayRightEdge = leftPos + imageWidth - BAY_OFFSET_X + 1;
        int trophyX = bayRightEdge - 18;
        int trophyY = topPos + 7;
        boolean trophyHover = mouseX >= trophyX - 2 && mouseX <= trophyX + 18 && mouseY >= trophyY - 1 && mouseY <= trophyY + 17;

        guiGraphics.fill(trophyX - 2, trophyY - 1, trophyX + 18, trophyY + 17, 0xFF2B170C);
        guiGraphics.fill(trophyX - 1, trophyY, trophyX + 17, trophyY + 16, trophyHover ? 0xFFFFE066 : 0xFFD4AF37);
        guiGraphics.fill(trophyX, trophyY + 1, trophyX + 16, trophyY + 15, 0xFF4A2E1B);
        if (trophyHover) guiGraphics.fill(trophyX, trophyY + 1, trophyX + 16, trophyY + 15, 0x44FFFFFF);
        guiGraphics.item(new ItemStack(Items.MOJANG_BANNER_PATTERN), trophyX, trophyY);

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

        if (mouseX >= bar1X && mouseX <= bar1X + barWidth && mouseY >= barY && mouseY <= barY + 10) {
            renderStreakTooltip(guiGraphics, mouseX, mouseY);
        }

        if (mouseX >= bar2X && mouseX <= bar2X + barWidth && mouseY >= barY && mouseY <= barY + 10) {
            renderBonusTooltip(guiGraphics, data.totalCollected, mouseX, mouseY);
        }

        int[] thresholds = {7, 14, 21};
        String rewLabel = Component.translatable("r3ct_daily.quests.tooltip.reward").getString();

        DailyServerConfig.MilestoneReward[] milestoneRewards = {
                DailyServerConfig.bonuses.bonus_7,
                DailyServerConfig.bonuses.bonus_14,
                DailyServerConfig.bonuses.bonus_21
        };

        String[] titles = new String[3];
        String[] rewards = new String[3];
        String[] amountsStr = new String[3];

        for (int i = 0; i < 3; i++) {
            String rewardColor = milestoneRewards[i].getFormattedColor();

            titles[i] = rewardColor + "§l" + Component.translatable("r3ct_daily.rewards.tooltip.bonus.title", thresholds[i]).getString();
            ItemStack stack = QuestManager.getMilestoneRewardStack(milestoneRewards[i]);
            rewards[i] = "§f" + rewLabel + " " + rewardColor + milestoneRewards[i].amount + "x " + stack.getHoverName().getString();
            amountsStr[i] = rewardColor + "x" + milestoneRewards[i].amount;
        }

        for (int i = 0; i < 3; i++) {
            int threshold = thresholds[i];
            int mX = (threshold == 21) ? (bar2X + barWidth - 1) : (bar2X + (int)(threshold * (barWidth / 21.0)));
            int totalW = 18 + this.font.width(amountsStr[i]);
            int startX = mX - totalW / 2;
            int startY = barY + 24;

            if (mouseX >= startX - 2 && mouseX <= startX + totalW + 2 && mouseY >= startY - 2 && mouseY <= startY + 18) {
                renderSimpleTooltip(guiGraphics, titles[i], rewards[i], mouseX, mouseY);
            }
        }

        if (trophyHover) {
            List<ClientTooltipComponent> tooltipLines = new ArrayList<>();
            tooltipLines.add(ClientTooltipComponent.create(Component.literal("§6§l" + Component.translatable("r3ct_daily.quests.tooltip.leaderboard.title").getString()).getVisualOrderText()));
            tooltipLines.add(ClientTooltipComponent.create(Component.literal("§8----------------").getVisualOrderText()));
            tooltipLines.add(ClientTooltipComponent.create(Component.literal("§f" + Component.translatable("r3ct_daily.quests.tooltip.leaderboard.desc").getString()).getVisualOrderText()));
            guiGraphics.tooltip(this.font, tooltipLines, mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null, true);
        }

        guiGraphics.pose().popMatrix();
    }

    private void drawTreasureChestCoffer(GuiGraphicsExtractor g, int leftPos, int topPos) {
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

    private void drawGoldCorner(GuiGraphicsExtractor g, int x, int y, boolean left, boolean top) {
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

    private void drawDayPedestalSlot(GuiGraphicsExtractor g, int slotX, int slotY, int day, int textWidth, boolean isClaimed, boolean canClaim, boolean isHovered, long time) {
        int centerX = slotX + 9;
        int plaqueHalfW = Math.max(20, (textWidth / 2) + 4);
        int plaqueLeft = centerX - plaqueHalfW;
        int plaqueRight = centerX + plaqueHalfW;

        g.fill(slotX - 2, slotY - 1, slotX + 20, slotY + 20, 0x44000000);
        g.fill(plaqueLeft + 1, slotY + 22, plaqueRight + 1, slotY + 36, 0x44000000);

        int trimColor;
        if (isClaimed) {
            trimColor = 0xFF4A3B3E;
        } else if (day <= 4) {
            trimColor = 0xFFA87948;
        } else if (day <= 6) {
            trimColor = 0xFF48A0B8;
        } else {
            trimColor = 0xFFC85AD4;
        }

        if (canClaim) {
            int pulseAlpha = (int) (130 + 80 * Math.sin(time / 160.0));
            int glow = (pulseAlpha << 24) | 0xFFAA00;
            g.fill(slotX - 3, slotY - 3, slotX + 21, slotY + 21, glow);
            trimColor = 0xFFFFD700;
        }

        g.fill(plaqueLeft, slotY + 21, plaqueRight, slotY + 35, 0xFF1A090D);
        g.fill(plaqueLeft + 1, slotY + 22, plaqueRight - 1, slotY + 34, isClaimed ? 0xFF2B1A1E : 0xFF3E1923);
        g.fill(plaqueLeft + 1, slotY + 22, plaqueRight - 1, slotY + 23, trimColor);

        g.fill(slotX - 1, slotY - 1, slotX + 19, slotY + 19, 0xFF1A090D);
        g.fill(slotX, slotY, slotX + 18, slotY + 18, trimColor);

        int innerColor = isClaimed ? 0xFF1F1719 : (canClaim ? 0xFF4A3210 : 0xFF26121A);
        g.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, innerColor);

        g.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 2, 0x55000000);
        g.fill(slotX + 1, slotY + 1, slotX + 2, slotY + 17, 0x55000000);

        if (isHovered) {
            g.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, 0x55FFFFFF);
        }
    }

    private void renderBonusMilestones(GuiGraphicsExtractor g, int x, int y, int barWidth, int mouseX, int mouseY) {
        int[] thresholds = {7, 14, 21};
        DailyServerConfig.MilestoneReward[] milestoneRewards = {
                DailyServerConfig.bonuses.bonus_7,
                DailyServerConfig.bonuses.bonus_14,
                DailyServerConfig.bonuses.bonus_21
        };

        ItemStack[] icons = new ItemStack[3];
        String[] amounts = new String[3];

        for (int i = 0; i < 3; i++) {
            icons[i] = QuestManager.getMilestoneRewardStack(milestoneRewards[i]);
            amounts[i] = milestoneRewards[i].getFormattedColor() + "x" + milestoneRewards[i].amount;
        }

        long time = System.currentTimeMillis();
        int cycle = RewardManager.getActiveBonusCycle(data);

        for (int i = 0; i < 3; i++) {
            int threshold = thresholds[i];
            int absTarget = cycle * 21 + threshold;
            boolean canClaim = data.totalCollected >= absTarget && !data.claimedBonusRewards.contains(absTarget);
            boolean claimed = data.claimedBonusRewards.contains(absTarget);

            int mX = (threshold == 21) ? (x + barWidth - 1) : (x + (int)(threshold * (barWidth / 21.0)));

            g.fill(mX, y - 2, mX + 1, y + 10, 0xFFFFFFFF);
            String txtDay = "§8" + threshold;
            g.text(this.font, txtDay, mX - (this.font.width(String.valueOf(threshold)) / 2), y + 12, 0xFFFFFFFF, false);

            int totalW = 18 + this.font.width(amounts[i]);
            int startX = mX - totalW / 2;
            int startY = y + 24;

            boolean hovered = mouseX >= startX - 2 && mouseX <= startX + totalW + 2 && mouseY >= startY - 2 && mouseY <= startY + 18;

            if (claimed) {
                g.fill(startX - 2, startY - 2, startX + totalW + 2, startY + 18, 0x33000000);
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
        String todayDate = getCurrentQuestDateString();
        boolean alreadyClaimedToday = todayDate.equals(data.lastRewardDate);
        boolean isClaimed = alreadyClaimedToday ? ((data.rewardDay == 1) || (day < data.rewardDay)) : (day < data.rewardDay);
        boolean canClaim = (day == data.rewardDay) && !alreadyClaimedToday;

        List<ClientTooltipComponent> tooltip = new ArrayList<>();
        tooltip.add(ClientTooltipComponent.create(Component.literal("§6§l" + Component.translatable("r3ct_daily.rewards.tooltip.day.title", day).getString()).getVisualOrderText()));

        String statusRaw = isClaimed ? "§a" + Component.translatable("r3ct_daily.rewards.status.claimed").getString() : (canClaim ? "§e" + Component.translatable("r3ct_daily.rewards.status.claimable").getString() : "§c" + Component.translatable("r3ct_daily.rewards.status.locked").getString());
        tooltip.add(ClientTooltipComponent.create(Component.literal(Component.translatable("r3ct_daily.rewards.tooltip.status").getString() + " " + statusRaw).getVisualOrderText()));

        tooltip.add(ClientTooltipComponent.create(Component.literal("§8----------------").getVisualOrderText()));

        if (isClaimed) {
            String historyEntry = (data.claimedRewardHistory != null && data.claimedRewardHistory.size() > day - 1) ? data.claimedRewardHistory.get(day - 1) : "";
            if (historyEntry != null && !historyEntry.isEmpty()) {
                tooltip.add(ClientTooltipComponent.create(Component.literal("§f" + Component.translatable("r3ct_daily.rewards.tooltip.content").getString()).getVisualOrderText()));

                String[] items = historyEntry.split(",");
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

        if (canClaim && data.rewardStreak >= 7) {
            tooltip.add(ClientTooltipComponent.create(Component.literal("§6★ " + Component.translatable("r3ct_daily.rewards.tooltip.bonus_loot").getString()).getVisualOrderText()));
        }

        guiGraphics.tooltip(this.font, tooltip, mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null, true);
    }

    private void renderStreakTooltip(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        List<ClientTooltipComponent> tooltip = new ArrayList<>();
        tooltip.add(ClientTooltipComponent.create(Component.literal("§6§l" + Component.translatable("r3ct_daily.rewards.tooltip.streak.title").getString()).getVisualOrderText()));
        tooltip.add(ClientTooltipComponent.create(Component.literal("§8----------------").getVisualOrderText()));

        tooltip.add(ClientTooltipComponent.create(Component.literal("§f" + Component.translatable("r3ct_daily.rewards.tooltip.streak.req1").getString()).getVisualOrderText()));
        tooltip.add(ClientTooltipComponent.create(Component.literal("§f" + Component.translatable("r3ct_daily.rewards.tooltip.streak.req2").getString()).getVisualOrderText()));

        tooltip.add(ClientTooltipComponent.create(Component.literal("").getVisualOrderText()));
        tooltip.add(ClientTooltipComponent.create(Component.literal("§b" + Component.translatable("r3ct_daily.gui.streak_freeze_title").getString()).getVisualOrderText()));
        tooltip.add(ClientTooltipComponent.create(Component.literal("").getVisualOrderText()));
        tooltip.add(ClientTooltipComponent.create(Component.literal("§f" + Component.translatable("r3ct_daily.rewards.tooltip.streak.freeze_desc1").getString()).getVisualOrderText()));
        tooltip.add(ClientTooltipComponent.create(Component.literal("§f" + Component.translatable("r3ct_daily.rewards.tooltip.streak.freeze_desc2").getString()).getVisualOrderText()));
        tooltip.add(ClientTooltipComponent.create(Component.literal("").getVisualOrderText()));

        int maxRewardShields = payload.maxRewardShields();
        tooltip.add(ClientTooltipComponent.create(Component.literal("§f" + Component.translatable("r3ct_daily.gui.available_shields").getString() + " §b" + data.availableRewardShields + "§f/" + maxRewardShields).getVisualOrderText()));

        guiGraphics.tooltip(this.font, tooltip, mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null, true);
    }

    private void renderBonusTooltip(GuiGraphicsExtractor guiGraphics, int absoluteCollected, int mouseX, int mouseY) {
        List<ClientTooltipComponent> tooltip = new ArrayList<>();
        tooltip.add(ClientTooltipComponent.create(Component.literal("§d§l" + Component.translatable("r3ct_daily.rewards.tooltip.bonus_main.title").getString()).getVisualOrderText()));
        tooltip.add(ClientTooltipComponent.create(Component.literal("§8----------------").getVisualOrderText()));

        DailyServerConfig.MilestoneReward[] milestoneRewards = {
                DailyServerConfig.bonuses.bonus_7,
                DailyServerConfig.bonuses.bonus_14,
                DailyServerConfig.bonuses.bonus_21
        };
        int[] thresholds = {7, 14, 21};

        for (int i = 0; i < 3; i++) {
            ItemStack stack = QuestManager.getMilestoneRewardStack(milestoneRewards[i]);
            String label = milestoneRewards[i].amount + "x " + stack.getHoverName().getString();
            tooltip.add(ClientTooltipComponent.create(
                    getBonusLine(absoluteCollected, thresholds[i], label, milestoneRewards[i].getFormattedColor()).getVisualOrderText()
            ));
        }

        guiGraphics.tooltip(this.font, tooltip, mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null, true);
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

    private void renderSimpleTooltip(GuiGraphicsExtractor guiGraphics, String title, String info, int mouseX, int mouseY) {
        List<ClientTooltipComponent> tooltip = new ArrayList<>();
        tooltip.add(ClientTooltipComponent.create(Component.literal(title).getVisualOrderText()));
        tooltip.add(ClientTooltipComponent.create(Component.literal("§8----------------").getVisualOrderText()));
        tooltip.add(ClientTooltipComponent.create(Component.literal(info).getVisualOrderText()));
        guiGraphics.tooltip(this.font, tooltip, mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null, true);
    }

    private boolean isMouseOverSlot(double mouseX, double mouseY, int x, int y) {
        return mouseX >= x && mouseX <= x + 18 && mouseY >= y && mouseY <= y + 18;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
        if (event.button() == SDLMouse.SDL_BUTTON_LEFT) {
            float scale = DailyClientConfig.getInstance().rewardScreenScale;

            int mX = (int)((event.x() - this.width / 2f) / scale + this.width / 2f);
            int mY = (int)((event.y() - this.height / 2f) / scale + this.height / 2f);

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

            String todayDate = getCurrentQuestDateString();
            boolean alreadyClaimedToday = todayDate.equals(data.lastRewardDate);

            for (int i = 1; i <= 7; i++) {
                int slotX = getSlotX(leftPos, i);
                int slotY = topPos + 43;

                if (isMouseOverSlot(mX, mY, slotX, slotY) && i == data.rewardDay && !alreadyClaimedToday) {
                    if (Minecraft.getInstance().player != null) {
                        Minecraft.getInstance().player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
                        Minecraft.getInstance().player.connection.sendCommand("daily claimreward");
                        return true;
                    }
                }
            }

            int barY = topPos + 140;
            int bar2X = midX + 16;
            int barWidth = 135;
            int[] thresholds = {7, 14, 21};
            DailyServerConfig.MilestoneReward[] milestoneRewards = {
                    DailyServerConfig.bonuses.bonus_7,
                    DailyServerConfig.bonuses.bonus_14,
                    DailyServerConfig.bonuses.bonus_21
            };

            int cycle = RewardManager.getActiveBonusCycle(data);

            for (int i = 0; i < 3; i++) {
                int threshold = thresholds[i];
                String amountsStr = milestoneRewards[i].getFormattedColor() + "x" + milestoneRewards[i].amount;
                int mX_bonus = (threshold == 21) ? (bar2X + barWidth - 1) : (bar2X + (int)(threshold * (barWidth / 21.0)));
                int totalW = 18 + this.font.width(amountsStr);
                int startX = mX_bonus - totalW / 2;
                int startY = barY + 24;

                if (mX >= startX - 2 && mX <= startX + totalW + 2 && mY >= startY - 2 && mY <= startY + 18) {
                    int absTarget = cycle * 21 + threshold;
                    boolean canClaim = data.totalCollected >= absTarget && !data.claimedBonusRewards.contains(absTarget);

                    if (canClaim) {
                        if (this.minecraft != null && this.minecraft.player != null) {
                            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                            this.minecraft.player.connection.sendCommand("daily claimbonus " + threshold);
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