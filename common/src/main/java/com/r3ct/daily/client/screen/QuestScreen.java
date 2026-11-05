package com.r3ct.daily.client.screen;

import com.r3ct.daily.logic.QuestSubmitHelper;
import com.r3ct.daily.config.DailyClientConfig;
import com.r3ct.daily.network.OpenQuestsPayload;
import com.r3ct.daily.platform.Services;
import com.r3ct.daily.data.PlayerData;
import com.r3ct.daily.logic.Quest;
import com.r3ct.daily.logic.QuestManager;
import com.r3ct.daily.config.DailyServerConfig;
import com.r3ct.daily.network.RequestLeaderboardPayload;
import com.r3ct.daily.network.RerollQuestPayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.NonNull;
import org.lwjgl.sdl.SDLMouse;

import java.util.ArrayList;
import java.util.List;

public class QuestScreen extends Screen {
    private final PlayerData data;
    private final OpenQuestsPayload payload;

    private final int bookWidth = 440;
    private final int bookHeight = 340;

    private static final int STAR_COLUMN_WIDTH = 11;
    private static final int[] CARD_X_OFFSETS = {-2, 3, -3, 2, -1};

    private static float animatedDaily = 0.0f;
    private static float animatedStreak = 0.0f;
    private static float animatedPoints = 0.0f;

    public QuestScreen(PlayerData data, OpenQuestsPayload payload) {
        super(Component.translatable("r3ct_daily.quests.title"));
        this.data = data;
        this.payload = payload;
    }

    private int getCardLeft(int leftPos, int index) {
        int offset = (index >= 0 && index < CARD_X_OFFSETS.length) ? CARD_X_OFFSETS[index] : 0;
        return leftPos + 16 + offset;
    }

    private int getCardRight(int midX, int index) {
        int offset = (index >= 0 && index < CARD_X_OFFSETS.length) ? CARD_X_OFFSETS[index] : 0;
        return midX - 4 + offset;
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {

        float scale = DailyServerConfig.mechanics != null ? DailyClientConfig.getInstance().questScreenScale : 1.0f;

        mouseX = (int)((mouseX - this.width / 2f) / scale + this.width / 2f);
        mouseY = (int)((mouseY - this.height / 2f) / scale + this.height / 2f);

        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(this.width / 2f, this.height / 2f);
        guiGraphics.pose().scale(scale, scale);
        guiGraphics.pose().translate(-this.width / 2f, -this.height / 2f);

        int leftPos = (this.width - bookWidth) / 2;
        int topPos = (this.height - bookHeight) / 2;
        int midX = leftPos + (bookWidth / 2);

        drawMedievalNoticeBoard(guiGraphics, leftPos, topPos);

        drawLeftHeaderPlaque(guiGraphics, leftPos + 22, topPos + 12, midX - 10, topPos + 34);
        String leftHeaderTitle = "§0§l" + Component.translatable("r3ct_daily.quests.header.daily_quests").getString();
        int leftPlaqueCenterX = (leftPos + 22 + midX - 10) / 2;
        guiGraphics.text(this.font, leftHeaderTitle, leftPlaqueCenterX - (this.font.width(leftHeaderTitle) / 2), topPos + 19, 0xFF000000, false);

        int hoveredQuestIndex = -1;
        int hoveredRerollIndex = -1;
        long time = System.currentTimeMillis();

        for (int i = 0; i < data.activeQuests.size(); i++) {
            Quest quest = QuestManager.getQuestById(data.activeQuests.get(i));
            if (quest == null) continue;

            int progress = data.questProgress.get(i);
            boolean done = progress >= quest.requiredAmount;
            boolean claimed = data.questRewardsClaimed.size() > i && data.questRewardsClaimed.get(i);
            boolean canSubmit = !done && quest.actionType.equals("SUBMIT_ITEMS") && QuestSubmitHelper.countItems(quest.target) > 0;

            String localizedName = I18n.get(quest.name);
            String localizedDesc = I18n.get(quest.description);

            String name = (localizedName != null && !localizedName.isEmpty()) ? localizedName.toUpperCase() : localizedDesc.split(" ")[0].toUpperCase();
            int questY = topPos + 45 + (i * 55);

            int cardLeft = getCardLeft(leftPos, i);
            int cardRight = getCardRight(midX, i);
            int cardTop = questY - 4;
            int cardBottom = questY + 45;
            int leftTextX = cardLeft + 6;

            int buttonSize = 14;
            int buttonX = cardRight - 18;
            int buttonY = questY + 23;
            boolean isBtnHovered = !done && payload.enableQuestRerolling()
                    && mouseX >= buttonX && mouseX <= buttonX + buttonSize
                    && mouseY >= buttonY && mouseY <= buttonY + buttonSize;

            boolean isCardHovered = mouseX >= cardLeft && mouseX <= cardRight
                    && mouseY >= cardTop && mouseY <= cardBottom && !isBtnHovered;

            if (isBtnHovered) hoveredRerollIndex = i;
            if (isCardHovered) hoveredQuestIndex = i;

            drawQuestNoteCard(guiGraphics, cardLeft, cardTop, cardRight, cardBottom, i, quest.difficulty, done, claimed, canSubmit, isCardHovered, time);

            if (!done && payload.enableQuestRerolling()) drawRerollSealButton(guiGraphics, buttonX, buttonY, buttonSize, isBtnHovered);

            String diffIndicator = (quest.difficulty == 0) ? "§2★" : (quest.difficulty == 1 ? "§6★" : "§4★");
            int titleColor = (done && !claimed) ? 0xFF005500 : (canSubmit ? 0xFF0044AA : 0xFF000000);

            String statusMark;
            if (!done) {
                if (canSubmit) {
                    statusMark = (time % 1000 < 500) ? "§f" + Component.translatable("r3ct_daily.gui.submit_items").getString() : "§b" + Component.translatable("r3ct_daily.gui.submit_items").getString();
                } else {
                    statusMark = "§c" + Component.translatable("r3ct_daily.quests.status.incomplete").getString();
                }
            } else if (claimed) {
                statusMark = "§a" + Component.translatable("r3ct_daily.quests.status.claimed").getString();
            } else {
                statusMark = (time % 1000 < 500) ? "§e" + Component.translatable("r3ct_daily.quests.status.claim").getString() : "§6" + Component.translatable("r3ct_daily.quests.status.claim").getString();
            }

            int markWidth = this.font.width(statusMark);
            int statusMarkX = cardRight - markWidth - 5;

            int titleStartX = leftTextX + STAR_COLUMN_WIDTH;
            int maxTitleWidth = statusMarkX - titleStartX - 5;

            String displayName = name;
            if (this.font.width(displayName) > maxTitleWidth) {
                displayName = this.font.plainSubstrByWidth(displayName, Math.max(0, maxTitleWidth - this.font.width("..."))) + "...";
            }

            guiGraphics.text(this.font, diffIndicator, leftTextX, questY + 1, titleColor, false);
            guiGraphics.text(this.font, "§0" + displayName, titleStartX, questY + 1, titleColor, false);
            guiGraphics.text(this.font, statusMark, statusMarkX, questY + 1, 0xFF000000, false);

            guiGraphics.fill(leftTextX, questY + 10, cardRight - 5, questY + 11, 0x338D6E63);

            int color = done ? 0xFF555555 : (quest.difficulty == 0 ? 0xFF008800 : (quest.difficulty == 1 ? 0xFFD97700 : 0xFFAA0000));
            String progressText = " (" + progress + "/" + quest.requiredAmount + ")";
            int maxTextWidth = (cardRight - 35) - (leftTextX + STAR_COLUMN_WIDTH);

            String displayDesc = localizedDesc;
            List<FormattedCharSequence> lines = this.font.split(Component.literal(displayDesc + progressText), maxTextWidth);

            if (lines.size() > 3) {
                int charsToKeep = (int)((double)localizedDesc.length() * ((double)(maxTextWidth * 3 - this.font.width("..." + progressText)) / Math.max(1, this.font.width(localizedDesc))));
                charsToKeep = Math.max(0, Math.min(charsToKeep, localizedDesc.length()));

                displayDesc = localizedDesc.substring(0, charsToKeep) + "...";
                lines = this.font.split(Component.literal(displayDesc + progressText), maxTextWidth);

                while (lines.size() > 3 && displayDesc.length() > 4) {
                    displayDesc = displayDesc.substring(0, displayDesc.length() - 5) + "...";
                    lines = this.font.split(Component.literal(displayDesc + progressText), maxTextWidth);
                }
            }

            for (int lineIdx = 0; lineIdx < lines.size(); lineIdx++) {
                guiGraphics.text(this.font, lines.get(lineIdx), leftTextX + STAR_COLUMN_WIDTH, questY + 13 + (lineIdx * 10), color, false);
            }
        }

        int rightSheetLeft = midX + 14;
        int rightSheetRight = leftPos + bookWidth - 18;
        int rightSheetTop = topPos + 22;
        int rightSheetBottom = topPos + bookHeight - 18;
        drawRightHangingScroll(guiGraphics, rightSheetLeft, rightSheetTop, rightSheetRight, rightSheetBottom, topPos);

        int rightTextX = midX + 26;
        int barWidth = 160;

        guiGraphics.text(this.font, "§0§l" + Component.translatable("r3ct_daily.quests.header.progress").getString(), rightTextX, topPos + 32, 0xFF000000, false);
        guiGraphics.fill(rightTextX, topPos + 44, rightSheetRight - 12, topPos + 45, 0xFF8D6E63);

        int dailyY = topPos + 80;
        int targetDaily = Math.min(data.dailyQuestsCompletedToday, 3);
        animatedDaily += (targetDaily - animatedDaily) * 0.1f;
        if (Math.abs(targetDaily - animatedDaily) < 0.05f) animatedDaily = targetDaily;

        String dailyText = "§2" + targetDaily + "§0/3";
        GuiUtils.drawRewardStyleBar(guiGraphics, this.font, rightTextX, dailyY, animatedDaily, 3, Component.translatable("r3ct_daily.quests.bar.daily").getString(), dailyText, 0xFF55FF55, barWidth, 1, new int[]{});

        int streakY = topPos + 155;
        int targetStreak = Math.min(data.questStreak, 7);
        animatedStreak += (targetStreak - animatedStreak) * 0.1f;
        if (Math.abs(targetStreak - animatedStreak) < 0.05f) animatedStreak = targetStreak;

        String streakColor = (targetStreak < 3) ? "§2" : (targetStreak < 7 ? "§6" : "§c");
        String streakText = streakColor + data.questStreak + "§0/7";
        int streakBarColor = (targetStreak < 3) ? 0xFF006400 : (targetStreak < 7 ? 0xFFFFAA00 : 0xFFFF5555);
        GuiUtils.drawRewardStyleBar(guiGraphics, this.font, rightTextX, streakY, animatedStreak, 7, Component.translatable("r3ct_daily.quests.bar.streak").getString(), streakText, streakBarColor, barWidth, 1, new int[]{});

        String qMultiText = "§8" + Component.translatable("r3ct_daily.gui.streak_bonus").getString() + (data.questStreak >= 7 ? "§a" + Component.translatable("r3ct_daily.gui.active").getString() : "§8" + Component.translatable("r3ct_daily.gui.inactive").getString());
        guiGraphics.text(this.font, qMultiText, rightTextX, streakY + 14, 0xFF000000, false);

        int maxQuestShields = payload.maxStoredQuestShields();
        String shieldsText = "§8" + Component.translatable("r3ct_daily.gui.available_shields").getString() + " §b" + data.availableQuestShields + "§8/" + maxQuestShields;
        guiGraphics.text(this.font, shieldsText, rightTextX, streakY + 26, 0xFF000000, false);

        int lifeY = topPos + 245;
        int targetPoints = Math.min(data.totalQuestPoints, 200);
        animatedPoints += (targetPoints - animatedPoints) * 0.1f;
        if (Math.abs(targetPoints - animatedPoints) < 0.05f) animatedPoints = targetPoints;

        String ptsText = "§d" + targetPoints + "§0/200";
        GuiUtils.drawRewardStyleBar(guiGraphics, this.font, rightTextX, lifeY, animatedPoints, 200, Component.translatable("r3ct_daily.quests.bar.points").getString(), ptsText, 0xFFFF55FF, barWidth, 10, new int[]{50, 100, 150, 200});

        renderPointMilestones(guiGraphics, rightTextX, lifeY, barWidth, mouseX, mouseY);

        String arrowText = Component.translatable("r3ct_daily.quests.button.rewards_next").getString();
        int textWidth = this.font.width(arrowText);
        int arrowX = leftPos + bookWidth + 15;
        int arrowY = (this.height / 2) - 4;

        boolean isHovered = mouseX >= arrowX && mouseX <= arrowX + textWidth && mouseY >= arrowY - 2 && mouseY <= arrowY + 10;
        int arrColor = isHovered ? 0xFFFFFFFF : 0xFFAAAAAA;
        guiGraphics.text(this.font, arrowText, arrowX, arrowY, arrColor, true);

        int signX = leftPos + bookWidth - 45;
        int signY = topPos - 26;
        int trophyX = signX + 5;
        int trophyY = signY + 4;

        boolean trophyHover = mouseX >= signX && mouseX <= signX + 26 && mouseY >= signY && mouseY <= signY + 28;

        drawLeaderboardSign(guiGraphics, signX, signY, trophyHover);
        guiGraphics.item(new ItemStack(Items.MOJANG_BANNER_PATTERN), trophyX, trophyY);

        String backText = Component.translatable("r3ct_daily.quests.button.close").getString();
        int backWidth = this.font.width(backText);
        int backX = midX - (backWidth / 2);
        int backY = topPos + bookHeight + 14;

        boolean backHover = mouseX >= backX - 2 && mouseX <= backX + backWidth + 2 && mouseY >= backY - 2 && mouseY <= backY + 10;
        int backColor = backHover ? 0xFFFF5555 : 0xFFAAAAAA;
        guiGraphics.text(this.font, backText, backX, backY, backColor, true);

        if (hoveredQuestIndex != -1) {
            renderQuestTooltip(guiGraphics, hoveredQuestIndex, mouseX, mouseY);
        }

        if (hoveredRerollIndex != -1) {
            Quest rq = QuestManager.getQuestById(data.activeQuests.get(hoveredRerollIndex));
            if (rq != null) {
                int cost = (rq.difficulty == 0) ? payload.rerollCostEasy() :
                        (rq.difficulty == 1) ? payload.rerollCostMedium() :
                                payload.rerollCostHard();

                List<ClientTooltipComponent> rTooltip = new ArrayList<>();
                rTooltip.add(ClientTooltipComponent.create(Component.literal("§e§l" + Component.translatable("r3ct_daily.quests.tooltip.reroll.title").getString()).getVisualOrderText()));
                rTooltip.add(ClientTooltipComponent.create(Component.literal("§8----------------").getVisualOrderText()));
                rTooltip.add(ClientTooltipComponent.create(Component.literal("§f" + Component.translatable("r3ct_daily.quests.tooltip.reroll.desc1").getString()).getVisualOrderText()));
                rTooltip.add(ClientTooltipComponent.create(Component.literal("§f" + Component.translatable("r3ct_daily.quests.tooltip.reroll.desc2").getString()).getVisualOrderText()));
                rTooltip.add(ClientTooltipComponent.create(Component.literal("").getVisualOrderText()));

                String costColor = (data.totalQuestPoints >= cost) ? "§a" : "§c";
                rTooltip.add(ClientTooltipComponent.create(Component.literal("§f" + Component.translatable("r3ct_daily.quests.tooltip.reroll.cost").getString() + " " + costColor + cost + " " + Component.translatable("r3ct_daily.unit.points").getString()).getVisualOrderText()));

                guiGraphics.tooltip(this.font, rTooltip, mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null, true);
            }
        }

        if (mouseX >= rightTextX && mouseX <= rightTextX + barWidth && mouseY >= dailyY && mouseY <= dailyY + 8) {
            renderDailyTooltip(guiGraphics, mouseX, mouseY);
        }

        if (mouseX >= rightTextX && mouseX <= rightTextX + barWidth && mouseY >= streakY && mouseY <= streakY + 8) {
            renderQuestStreakTooltip(guiGraphics, mouseX, mouseY);
        }

        int[] thresholds = {50, 100, 150, 200};
        String rewLabel = Component.translatable("r3ct_daily.quests.tooltip.reward").getString();

        DailyServerConfig.MilestoneReward[] milestoneRewards = {
                DailyServerConfig.milestones.point_50,
                DailyServerConfig.milestones.point_100,
                DailyServerConfig.milestones.point_150,
                DailyServerConfig.milestones.point_200
        };

        String[] tooltips = new String[4];
        String[] rewards = new String[4];
        String[] amountsStr = new String[4];

        for (int i = 0; i < 4; i++) {
            String color = milestoneRewards[i].getFormattedColor();

            tooltips[i] = color + "§l" + Component.translatable("r3ct_daily.quests.tooltip.points.threshold", thresholds[i]).getString();

            ItemStack stack = QuestManager.getMilestoneRewardStack(milestoneRewards[i]);
            rewards[i] = "§f" + rewLabel + " " + color + milestoneRewards[i].amount + "x " + stack.getHoverName().getString();
            amountsStr[i] = color + "x" + milestoneRewards[i].amount;
        }

        for (int i = 0; i < 4; i++) {
            int mX = rightTextX + (int)(thresholds[i] / 200.0 * (barWidth - 2));
            int totalW = 18 + this.font.width(amountsStr[i]);
            int startX = mX - totalW / 2;
            int startY = lifeY + 28;

            if (mouseX >= startX - 2 && mouseX <= startX + totalW + 2 && mouseY >= startY - 2 && mouseY <= startY + 18) {
                renderSimpleTooltip(guiGraphics, tooltips[i], rewards[i], mouseX, mouseY);
            }
        }

        if (mouseX >= rightTextX && mouseX <= rightTextX + barWidth && mouseY >= lifeY && mouseY <= lifeY + 8) {
            renderLifetimeTooltip(guiGraphics, mouseX, mouseY);
        }

        if (trophyHover) {
            List<ClientTooltipComponent> tTooltip = new ArrayList<>();
            tTooltip.add(ClientTooltipComponent.create(Component.literal("§6§l" + Component.translatable("r3ct_daily.quests.tooltip.leaderboard.title").getString()).getVisualOrderText()));
            tTooltip.add(ClientTooltipComponent.create(Component.literal("§8----------------").getVisualOrderText()));
            tTooltip.add(ClientTooltipComponent.create(Component.literal("§f" + Component.translatable("r3ct_daily.quests.tooltip.leaderboard.desc").getString()).getVisualOrderText()));
            guiGraphics.tooltip(this.font, tTooltip, mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null, true);
        }

        guiGraphics.pose().popMatrix();
    }

    private void drawRerollSealButton(GuiGraphicsExtractor g, int x, int y, int size, boolean isHovered) {
        int ribbonColor = isHovered ? 0xFFB71C1C : 0xFF8E1B1B;
        g.fill(x + 3, y + size - 1, x + 5, y + size + 2, ribbonColor);
        g.fill(x + size - 5, y + size - 1, x + size - 3, y + size + 2, ribbonColor);

        g.fill(x + 2, y + 2, x + size - 1, y + size, 0x3A000000);
        g.fill(x + 1, y + 3, x + size, y + size - 1, 0x3A000000);

        int outerRim = isHovered ? 0xFF7A5216 : 0xFF5C3D18;
        int ringHighlight = isHovered ? 0xFFFFE57F : 0xFFD4A349;
        int innerRecess = isHovered ? 0xFFC68A2E : 0xFF966827;
        int innerShadow = isHovered ? 0xFF8C5B14 : 0xFF6B4716;
        int iconColor = isHovered ? 0xFFFFFFFF : 0xFFFFF3D1;

        g.fill(x + 2, y, x + size - 2, y + size, outerRim);
        g.fill(x, y + 2, x + size, y + size - 2, outerRim);
        g.fill(x + 1, y + 1, x + size - 1, y + size - 1, outerRim);

        g.fill(x + 2, y + 1, x + size - 2, y + size - 1, ringHighlight);
        g.fill(x + 1, y + 2, x + size - 1, y + size - 2, ringHighlight);

        g.fill(x + 3, y + 2, x + size - 3, y + size - 2, innerRecess);
        g.fill(x + 2, y + 3, x + size - 2, y + size - 3, innerRecess);
        g.fill(x + 3, y + 2, x + size - 3, y + 3, innerShadow);

        String arrow = "⇄";
        int textX = x + (size - this.font.width(arrow)) / 2 + 1;
        int textY = y + 3;
        g.text(this.font, arrow, textX, textY + 1, outerRim, false);
        g.text(this.font, arrow, textX, textY, iconColor, false);
    }

    private void drawMedievalNoticeBoard(GuiGraphicsExtractor g, int leftPos, int topPos) {
        int rightPos = leftPos + bookWidth;
        int bottomPos = topPos + bookHeight;

        drawSupportPost(g, leftPos - 8, topPos - 10, leftPos + 6, bottomPos + 10);
        drawSupportPost(g, rightPos - 6, topPos - 10, rightPos + 8, bottomPos + 10);

        int innerLeft = leftPos + 6;
        int innerTop = topPos + 5;
        int innerRight = rightPos - 6;
        int innerBottom = bottomPos - 5;

        g.fill(innerLeft - 1, innerTop - 1, innerRight + 1, innerBottom + 1, 0xFF1E1008);

        int[] plankColors = {0xFF5D3A22, 0xFF54331D, 0xFF633E24, 0xFF4E2F1A};
        int plankHeight = 28;
        int plankIdx = 0;

        for (int py = innerTop; py < innerBottom; py += plankHeight) {
            int pBottom = Math.min(py + plankHeight, innerBottom);
            int baseColor = plankColors[plankIdx % plankColors.length];

            g.fill(innerLeft, py, innerRight, pBottom, baseColor);

            g.fill(innerLeft, py, innerRight, py + 1, 0xFF72492C);
            g.fill(innerLeft, pBottom - 1, innerRight, pBottom, 0xFF2B180C);

            int grainY = py + (plankHeight / 2);
            if (grainY < pBottom - 2) {
                int grainStart = innerLeft + ((plankIdx * 53) % 90);
                int grainEnd = grainStart + 70 + ((plankIdx * 31) % 80);
                g.fill(grainStart, grainY, Math.min(grainEnd, innerRight - 10), grainY + 1, 0x332B180C);
            }

            int joint1 = innerLeft + 75 + ((plankIdx * 97) % 140);
            int joint2 = innerLeft + 240 + ((plankIdx * 67) % 150);
            g.fill(joint1, py, joint1 + 1, pBottom, 0xFF2B180C);
            g.fill(joint1 + 1, py + 1, joint1 + 2, pBottom - 1, 0xFF6E472B);
            if (joint2 < innerRight - 20) {
                g.fill(joint2, py, joint2 + 1, pBottom, 0xFF2B180C);
                g.fill(joint2 + 1, py + 1, joint2 + 2, pBottom - 1, 0xFF6E472B);
            }

            plankIdx++;
        }

        g.fill(innerLeft, innerTop, innerRight, innerTop + 6, 0x66000000);
        g.fill(innerLeft, innerTop, innerLeft + 4, innerBottom, 0x55000000);
        g.fill(innerRight - 4, innerTop, innerRight, innerBottom, 0x55000000);
        g.fill(innerLeft, innerBottom - 3, innerRight, innerBottom, 0x44000000);

        g.fill(leftPos - 12, topPos - 7, rightPos + 12, topPos + 6, 0xFF1A0E07);
        g.fill(leftPos - 11, topPos - 6, rightPos + 11, topPos + 5, 0xFF3E2414);
        g.fill(leftPos - 11, topPos - 6, rightPos + 11, topPos - 3, 0xFF59351F);
        g.fill(leftPos - 11, topPos + 3, rightPos + 11, topPos + 5, 0xFF28160C);

        g.fill(leftPos - 9, bottomPos - 5, rightPos + 9, bottomPos + 5, 0xFF1A0E07);
        g.fill(leftPos - 8, bottomPos - 4, rightPos + 8, bottomPos + 4, 0xFF3E2414);
        g.fill(leftPos - 8, bottomPos - 4, rightPos + 8, bottomPos - 2, 0xFF59351F);

        drawIronStrap(g, leftPos - 8, topPos + 10, 14);
        drawIronStrap(g, rightPos - 6, topPos + 10, 14);
        drawIronStrap(g, leftPos - 8, bottomPos - 15, 14);
        drawIronStrap(g, rightPos - 6, bottomPos - 15, 14);
    }

    private void drawSupportPost(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2) {
        g.fill(x1, y1, x2, y2, 0xFF180D06);
        g.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, 0xFF3A2112);
        g.fill(x1 + 2, y1 + 1, x1 + 4, y2 - 1, 0xFF52301B);
        g.fill(x2 - 3, y1 + 1, x2 - 1, y2 - 1, 0xFF26150B);

        g.fill(x1, y1, x2, y1 + 3, 0xFF3B3F46);
        g.fill(x1 + 1, y1 + 1, x2 - 1, y1 + 2, 0xFF686E78);
    }

    private void drawIronStrap(GuiGraphicsExtractor g, int x, int y, int width) {
        g.fill(x, y, x + width, y + 5, 0xFF26292E);
        g.fill(x + 1, y + 1, x + width - 1, y + 4, 0xFF484D56);
        g.fill(x + 1, y + 1, x + width - 1, y + 2, 0xFF6B7280);

        g.fill(x + 2, y + 2, x + 4, y + 4, 0xFFAAB2BD);
        g.fill(x + width - 4, y + 2, x + width - 2, y + 4, 0xFFAAB2BD);
    }

    private void drawLeftHeaderPlaque(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2) {
        g.fill(x1 + 2, y1 + 2, x2 + 2, y2 + 2, 0x66000000);

        g.fill(x1 - 2, y1 - 1, x2 + 2, y2 + 1, 0xFF2B170C);
        g.fill(x1 - 1, y1, x2 + 1, y2, 0xFF4A2B18);

        g.fill(x1 + 2, y1 + 1, x2 - 2, y2 - 1, 0xFFE6CEAA);
        g.fill(x1 + 3, y1 + 2, x2 - 3, y1 + 3, 0xFFF7E7C8);

        int midY = (y1 + y2) / 2;
        g.fill(x1 + 5, midY - 1, x1 + 7, midY + 1, 0xFF3B3F46);
        g.fill(x2 - 7, midY - 1, x2 - 5, midY + 1, 0xFF3B3F46);
    }

    private void drawQuestNoteCard(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int index, int difficulty, boolean done, boolean claimed, boolean canSubmit, boolean isHovered, long time) {
        g.fill(x1 + 3, y1 + 3, x2 + 3, y2 + 3, 0x70000000);

        int borderColor = claimed ? 0xFF7D6852 : 0xFFA88B63;
        int paperColor = claimed ? 0xFFD4C09E : ((index % 2 == 0) ? 0xFFF5E4C0 : 0xFFEEDBB4);
        int paperHighlight = claimed ? 0xFFE0CEAE : 0xFFFFF3D9;

        g.fill(x1, y1, x2, y2, borderColor);
        g.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, paperColor);
        g.fill(x1 + 1, y1 + 1, x2 - 1, y1 + 2, paperHighlight);

        g.fill(x2 - 5, y2 - 5, x2, y2, 0xFF4A2E1B);
        g.fill(x2 - 5, y2 - 5, x2 - 1, y2 - 1, 0xFFC7AF87);

        if ((done && !claimed) || canSubmit) {
            int alpha = (int) (95 + 50 * Math.sin(time / 150.0));
            int rgb = (done && !claimed) ? 0x00AA00 : 0x00AAFF;
            int glowColor = (alpha << 24) | rgb;
            g.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, glowColor);
        }

        if (isHovered) {
            g.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, 0x1A000000);
            g.fill(x1, y1, x2, y1 + 1, 0xFFFFFFFF);
            g.fill(x1, y2 - 1, x2, y2, 0xFFFFFFFF);
            g.fill(x1, y1, x1 + 1, y2, 0xFFFFFFFF);
            g.fill(x2 - 1, y1, x2, y2, 0xFFFFFFFF);
        }

        int nailOffset = (index % 2 == 0) ? -6 : 6;
        int pinX = (x1 + x2) / 2 + nailOffset;
        int pinY = y1 - 2;
        g.fill(pinX + 1, pinY + 1, pinX + 5, pinY + 5, 0x66000000);
        g.fill(pinX, pinY, pinX + 4, pinY + 4, 0xFF3B3F46);
        g.fill(pinX + 1, pinY + 1, pinX + 3, pinY + 2, 0xFF9EA6B2);

        int sealColor = (difficulty == 0) ? 0xFF2E7D32 : (difficulty == 1 ? 0xFFD97706 : 0xFFB91C1C);
        g.fill(x1 - 1, y1 + 4, x1 + 3, y1 + 12, sealColor);
    }

    private void drawLeaderboardSign(GuiGraphicsExtractor g, int x, int y, boolean isHovered) {
        g.fill(x + 2, y + 2, x + 28, y + 30, 0x55000000);

        g.fill(x, y, x + 26, y + 28, 0xFF1A0E07);
        g.fill(x + 1, y + 1, x + 25, y + 27, 0xFF4A2B18);
        g.fill(x + 2, y + 2, x + 24, y + 26, 0xFF633E24);

        if (isHovered) {
            g.fill(x + 2, y + 2, x + 24, y + 26, 0x22FFFFFF);
        }

        g.fill(x + 4, y + 22, x + 6, y + 24, 0xFF26292E);
        g.fill(x + 5, y + 23, x + 7, y + 25, 0xFF6B7280);

        g.fill(x + 20, y + 22, x + 22, y + 24, 0xFF26292E);
        g.fill(x + 21, y + 23, x + 23, y + 25, 0xFF6B7280);
    }

    private void drawRightHangingScroll(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int topPos) {
        int centerX = (x1 + x2) / 2;

        int nailY = y1 - 12;
        for (int step = 0; step < 8; step++) {
            g.fill(centerX - (step * 4), nailY + step + 2, centerX - (step * 4) + 2, nailY + step + 3, 0xFF8C7355);
            g.fill(centerX + (step * 4), nailY + step + 2, centerX + (step * 4) + 2, nailY + step + 3, 0xFF8C7355);
        }
        g.fill(centerX - 2, nailY, centerX + 3, nailY + 4, 0xFF26292E);
        g.fill(centerX - 1, nailY + 1, centerX + 2, nailY + 2, 0xFF9EA6B2);

        g.fill(x1 + 4, y1 + 3, x2 + 5, y2 + 6, 0x77000000);

        g.fill(x1, y1, x2, y2, 0xFF9E825C);
        g.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, 0xFFF2DCBA);
        g.fill(x1 + 3, y1 + 3, x2 - 3, y2 - 3, 0x338D6E63);
        g.fill(x1 + 4, y1 + 4, x2 - 4, y2 - 4, 0xFFF7E6C6);

        drawScrollRoller(g, x1 - 5, y1 - 4, x2 + 5, y1 + 3);
        drawScrollRoller(g, x1 - 5, y2 - 2, x2 + 5, y2 + 5);

        int divLeft = x1 + 12;
        int divRight = x2 - 12;
        g.fill(divLeft, topPos + 122, divRight, topPos + 123, 0x448D6E63);
        g.fill(divLeft, topPos + 212, divRight, topPos + 213, 0x448D6E63);
    }

    private void drawScrollRoller(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2) {
        g.fill(x1 + 2, y1 + 2, x2 + 2, y2 + 2, 0x55000000);
        g.fill(x1 + 4, y1, x2 - 4, y2, 0xFF2B170C);
        g.fill(x1 + 5, y1 + 1, x2 - 5, y2 - 1, 0xFF52301B);
        g.fill(x1 + 5, y1 + 1, x2 - 5, y1 + 2, 0xFF70452A);
        g.fill(x1, y1 - 1, x1 + 6, y2 + 1, 0xFF7A5914);
        g.fill(x1 + 1, y1, x1 + 5, y2, 0xFFD4AF37);
        g.fill(x2 - 6, y1 - 1, x2, y2 + 1, 0xFF7A5914);
        g.fill(x2 - 5, y1, x2 - 1, y2, 0xFFD4AF37);
    }

    private void renderPointMilestones(GuiGraphicsExtractor g, int x, int y, int bWidth, int mouseX, int mouseY) {
        int[] thresholds = {50, 100, 150, 200};
        DailyServerConfig.MilestoneReward[] rewards = {
                DailyServerConfig.milestones.point_50,
                DailyServerConfig.milestones.point_100,
                DailyServerConfig.milestones.point_150,
                DailyServerConfig.milestones.point_200
        };

        ItemStack[] icons = new ItemStack[4];
        String[] amounts = new String[4];

        for (int i = 0; i < 4; i++) {
            icons[i] = QuestManager.getMilestoneRewardStack(rewards[i]);
            amounts[i] = rewards[i].getFormattedColor() + "x" + rewards[i].amount;
        }

        long time = System.currentTimeMillis();

        for (int i = 0; i < 4; i++) {
            int t = thresholds[i];
            int mX = x + (int)(t / 200.0 * (bWidth - 2));
            boolean canClaim = data.totalQuestPoints >= t && !data.claimedPointRewards.contains(t);
            boolean claimed = data.claimedPointRewards.contains(t);

            int totalW = 18 + this.font.width(amounts[i]);
            int startX = mX - totalW / 2;
            int startY = y + 28;

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

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
        if (event.button() == SDLMouse.SDL_BUTTON_LEFT) {
            float scale = DailyClientConfig.getInstance().questScreenScale;

            int mX = (int)((event.x() - this.width / 2f) / scale + this.width / 2f);
            int mY = (int)((event.y() - this.height / 2f) / scale + this.height / 2f);

            int leftPos = (this.width - bookWidth) / 2;
            int topPos = (this.height - bookHeight) / 2;
            int midX = leftPos + (bookWidth / 2);

            int signX = leftPos + bookWidth - 45;
            int signY = topPos - 26;

            if (mX >= signX && mX <= signX + 26 && mY >= signY && mY <= signY + 28) {
                if (this.minecraft != null && this.minecraft.player != null) {
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    Services.PLATFORM.sendToServer(new RequestLeaderboardPayload(0));
                    return true;
                }
            }

            for (int i = 0; i < data.activeQuests.size(); i++) {
                int questY = topPos + 45 + (i * 55);
                Quest quest = QuestManager.getQuestById(data.activeQuests.get(i));
                if (quest == null) continue;

                int cardLeft = getCardLeft(leftPos, i);
                int cardRight = getCardRight(midX, i);

                int currentProgress = data.questProgress.get(i);
                boolean done = currentProgress >= quest.requiredAmount;
                boolean claimed = data.questRewardsClaimed.size() > i && data.questRewardsClaimed.get(i);

                int buttonSize = 14;
                int buttonX = cardRight - 18;
                int buttonY = questY + 23;

                if (!done && payload.enableQuestRerolling() && mX >= buttonX && mX <= buttonX + buttonSize && mY >= buttonY && mY <= buttonY + buttonSize) {
                    if (this.minecraft != null && this.minecraft.player != null) {
                        Services.PLATFORM.sendToServer(new RerollQuestPayload(i));
                        return true;
                    }
                }

                if (mX >= cardLeft && mX <= cardRight && mY >= questY - 4 && mY <= questY + 45) {
                    if (done && !claimed) {
                        if (this.minecraft != null && this.minecraft.player != null) {
                            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                            this.minecraft.player.connection.sendCommand("daily claimquest " + i);
                            return true;
                        }
                    } else if (!done && quest.actionType.equals("SUBMIT_ITEMS")) {
                        if (this.minecraft != null && this.minecraft.player != null) {
                            if (QuestSubmitHelper.countItems(quest.target) > 0) {
                                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                                QuestSubmitHelper.handleQuestSubmitClick(this, quest, i, currentProgress);
                                return true;
                            }
                        }
                    }
                }
            }

            int rightTextX = midX + 26;
            int barWidth = 160;
            int lifeY = topPos + 245;
            int[] thresholds = {50, 100, 150, 200};

            DailyServerConfig.MilestoneReward[] milestoneRewards = {
                    DailyServerConfig.milestones.point_50,
                    DailyServerConfig.milestones.point_100,
                    DailyServerConfig.milestones.point_150,
                    DailyServerConfig.milestones.point_200
            };

            for (int i = 0; i < 4; i++) {
                int t = thresholds[i];
                String amountsStr = milestoneRewards[i].getFormattedColor() + "x" + milestoneRewards[i].amount;

                int iconX = rightTextX + (int)(t / 200.0 * (barWidth - 2));
                int totalW = 18 + this.font.width(amountsStr);
                int startX = iconX - totalW / 2;
                int startY = lifeY + 28;

                if (mX >= startX - 2 && mX <= startX + totalW + 2 && mY >= startY - 2 && mY <= startY + 18) {
                    boolean canClaim = data.totalQuestPoints >= t && !data.claimedPointRewards.contains(t);
                    if (canClaim) {
                        if (this.minecraft != null && this.minecraft.player != null) {
                            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                            this.minecraft.player.connection.sendCommand("daily claimpoints " + t);
                            return true;
                        }
                    }
                }
            }

            String arrowText = Component.translatable("r3ct_daily.quests.button.rewards_next").getString();
            int textWidth = this.font.width(arrowText);
            int arrowX = leftPos + bookWidth + 15;
            int arrowY = (this.height / 2) - 4;

            if (mX >= arrowX && mX <= arrowX + textWidth && mY >= arrowY - 2 && mY <= arrowY + 10) {
                if (this.minecraft != null && this.minecraft.player != null) {
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
                    this.minecraft.player.connection.sendCommand("daily rewards");
                    return true;
                }
            }

            String backText = Component.translatable("r3ct_daily.quests.button.close").getString();
            int backWidth = this.font.width(backText);
            int backX = midX - (backWidth / 2);
            int backY = topPos + bookHeight + 14;

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

    private void renderQuestStreakTooltip(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        List<ClientTooltipComponent> tooltip = new ArrayList<>();
        tooltip.add(ClientTooltipComponent.create(Component.literal("§6§l" + Component.translatable("r3ct_daily.quests.tooltip.streak.title").getString()).getVisualOrderText()));
        tooltip.add(ClientTooltipComponent.create(Component.literal("§8----------------").getVisualOrderText()));

        String multiVal = String.valueOf(payload.questStreakXpMultiplier()).replace(".0", "");
        tooltip.add(ClientTooltipComponent.create(Component.literal("§f" + Component.translatable("r3ct_daily.quests.tooltip.streak.req1").getString()).getVisualOrderText()));
        tooltip.add(ClientTooltipComponent.create(Component.literal("§f" + Component.translatable("r3ct_daily.quests.tooltip.streak.req2", multiVal).getString()).getVisualOrderText()));

        tooltip.add(ClientTooltipComponent.create(Component.literal("").getVisualOrderText()));
        tooltip.add(ClientTooltipComponent.create(Component.literal("§b" + Component.translatable("r3ct_daily.gui.streak_freeze_title").getString()).getVisualOrderText()));
        tooltip.add(ClientTooltipComponent.create(Component.literal("").getVisualOrderText()));
        int reqDays = payload.perfectDaysForShield();
        tooltip.add(ClientTooltipComponent.create(Component.literal("§f" + Component.translatable("r3ct_daily.quests.tooltip.streak.freeze_desc1", reqDays).getString()).getVisualOrderText()));
        tooltip.add(ClientTooltipComponent.create(Component.literal("§f" + Component.translatable("r3ct_daily.quests.tooltip.streak.freeze_desc2").getString()).getVisualOrderText()));
        tooltip.add(ClientTooltipComponent.create(Component.literal("").getVisualOrderText()));

        int maxQuestShields = payload.maxStoredQuestShields();
        tooltip.add(ClientTooltipComponent.create(Component.literal("§f" + Component.translatable("r3ct_daily.gui.available_shields").getString() + " §b" + data.availableQuestShields + "§f/" + maxQuestShields).getVisualOrderText()));

        tooltip.add(ClientTooltipComponent.create(Component.literal("§f" + Component.translatable("r3ct_daily.quests.tooltip.streak.progress").getString() + " §b" + data.perfectDaysCount + "§f/" + reqDays).getVisualOrderText()));

        guiGraphics.tooltip(this.font, tooltip, mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null, true);
    }

    private void renderSimpleTooltip(GuiGraphicsExtractor guiGraphics, String title, String info, int mouseX, int mouseY) {
        List<ClientTooltipComponent> tooltip = new ArrayList<>();
        tooltip.add(ClientTooltipComponent.create(Component.literal(title).getVisualOrderText()));
        tooltip.add(ClientTooltipComponent.create(Component.literal("§8----------------").getVisualOrderText()));
        tooltip.add(ClientTooltipComponent.create(Component.literal(info).getVisualOrderText()));
        guiGraphics.tooltip(this.font, tooltip, mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null, true);
    }

    private void renderQuestTooltip(GuiGraphicsExtractor guiGraphics, int index, int mouseX, int mouseY) {
        Quest quest = QuestManager.getQuestById(data.activeQuests.get(index));
        if (quest == null) return;

        float xpMulti = (data.questStreak >= 7) ? payload.questStreakXpMultiplier() : 1.0f;

        int baseXp = (quest.difficulty == 0) ? payload.xpPerQuestEasy() :
                (quest.difficulty == 1) ? payload.xpPerQuestMedium() :
                        payload.xpPerQuestHard();

        int xpReward = (int)(baseXp * xpMulti);

        int itemAmount = quest.rewardAmount;

        List<ClientTooltipComponent> tooltip = new ArrayList<>();

        String questTitle = Component.translatable("r3ct_daily.quests.tooltip.quest.title", (index + 1)).getString();
        tooltip.add(ClientTooltipComponent.create(Component.literal("§l" + questTitle).getVisualOrderText()));

        String diffName = (quest.difficulty == 0) ? "§2" + Component.translatable("r3ct_daily.quests.tooltip.quest.diff.0").getString() : ((quest.difficulty == 1) ? "§6" + Component.translatable("r3ct_daily.quests.tooltip.quest.diff.1").getString() : "§4" + Component.translatable("r3ct_daily.quests.tooltip.quest.diff.2").getString());
        tooltip.add(ClientTooltipComponent.create(Component.literal(Component.translatable("r3ct_daily.quests.tooltip.quest.diff_label").getString() + " " + diffName).getVisualOrderText()));

        tooltip.add(ClientTooltipComponent.create(Component.literal("§8----------------").getVisualOrderText()));

        String localizedDesc = I18n.get(quest.description);

        String descColor = (quest.difficulty == 0) ? "§2" : (quest.difficulty == 1 ? "§6" : "§4");
        String descPrefix = "§f" + Component.translatable("r3ct_daily.quests.tooltip.quest.desc").getString() + " " + descColor;

        List<FormattedCharSequence> descLines = this.font.split(Component.literal(descPrefix + localizedDesc), 200);
        for (FormattedCharSequence seq : descLines) {
            tooltip.add(ClientTooltipComponent.create(seq));
        }

        tooltip.add(ClientTooltipComponent.create(Component.literal("§f" + Component.translatable("r3ct_daily.quests.tooltip.quest.points").getString() + " §d+" + quest.points).getVisualOrderText()));
        tooltip.add(ClientTooltipComponent.create(Component.literal("§f" + Component.translatable("r3ct_daily.quests.tooltip.quest.xp").getString() + " §e+" + xpReward + " §e" + Component.translatable("r3ct_daily.unit.xp").getString()).getVisualOrderText()));
        tooltip.add(ClientTooltipComponent.create(Component.literal("§f" + Component.translatable("r3ct_daily.quests.tooltip.reward").getString() + " §b" + itemAmount + "§bx §b" + quest.getItemReward().getHoverName().getString()).getVisualOrderText()));

        guiGraphics.tooltip(this.font, tooltip, mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null, true);
    }

    private void renderDailyTooltip(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        List<ClientTooltipComponent> tooltip = new ArrayList<>();
        tooltip.add(ClientTooltipComponent.create(Component.literal("§f§l" + Component.translatable("r3ct_daily.quests.tooltip.daily.title").getString()).getVisualOrderText()));
        tooltip.add(ClientTooltipComponent.create(Component.literal("§8----------------").getVisualOrderText()));
        tooltip.add(ClientTooltipComponent.create(Component.literal("§f" + Component.translatable("r3ct_daily.quests.tooltip.daily.desc1").getString()).getVisualOrderText()));
        tooltip.add(ClientTooltipComponent.create(Component.literal("§f" + Component.translatable("r3ct_daily.quests.tooltip.daily.desc2").getString()).getVisualOrderText()));
        tooltip.add(ClientTooltipComponent.create(Component.literal("").getVisualOrderText()));

        float xpMulti = (data.questStreak >= 7) ? payload.questStreakXpMultiplier() : 1.0f;
        int dailyXp = (int)(payload.xpDailyReward() * xpMulti);

        tooltip.add(ClientTooltipComponent.create(Component.literal("§f" + Component.translatable("r3ct_daily.quests.tooltip.quest.xp").getString() + " §e+" + dailyXp + " §e" + Component.translatable("r3ct_daily.unit.xp").getString()).getVisualOrderText()));

        tooltip.add(ClientTooltipComponent.create(Component.literal("§f" + Component.translatable("r3ct_daily.quests.tooltip.daily.list_title").getString()).getVisualOrderText()));

        if (DailyServerConfig.dailyQuestRewards != null && !DailyServerConfig.dailyQuestRewards.isEmpty()) {
            for (DailyServerConfig.RewardEntry entry : DailyServerConfig.dailyQuestRewards) {

                Item item = BuiltInRegistries.ITEM.getOptional(
                        Identifier.parse(entry.item)
                ).orElse(Items.AIR);

                String itemName = new ItemStack(item).getHoverName().getString();

                String amountStr = (entry.minAmount == entry.maxAmount) ?
                        " (" + entry.minAmount + ")" :
                        " (" + entry.minAmount + "-" + entry.maxAmount + ")";

                String entryColor = entry.getFormattedColor();

                tooltip.add(ClientTooltipComponent.create(
                        Component.literal(entryColor + "- " + itemName + amountStr).getVisualOrderText()
                ));
            }
        }

        guiGraphics.tooltip(this.font, tooltip, mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null, true);
    }

    private void renderLifetimeTooltip(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        List<ClientTooltipComponent> tooltip = new ArrayList<>();
        tooltip.add(ClientTooltipComponent.create(Component.literal("§d§l" + Component.translatable("r3ct_daily.quests.tooltip.lifetime.title").getString()).getVisualOrderText()));
        tooltip.add(ClientTooltipComponent.create(Component.literal("§8----------------").getVisualOrderText()));

        DailyServerConfig.MilestoneReward[] milestoneRewards = {
                DailyServerConfig.milestones.point_50,
                DailyServerConfig.milestones.point_100,
                DailyServerConfig.milestones.point_150,
                DailyServerConfig.milestones.point_200
        };
        int[] thresholds = {50, 100, 150, 200};

        for (int i = 0; i < 4; i++) {
            ItemStack stack = QuestManager.getMilestoneRewardStack(milestoneRewards[i]);
            String label = milestoneRewards[i].amount + "x " + stack.getHoverName().getString();

            tooltip.add(ClientTooltipComponent.create(
                    getLifetimeTooltipLine(data.totalQuestPoints, thresholds[i], label, milestoneRewards[i].getFormattedColor()).getVisualOrderText()
            ));
        }

        guiGraphics.tooltip(this.font, tooltip, mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null, true);
    }

    private Component getLifetimeTooltipLine(int current, int target, String reward, String color) {
        boolean claimed = data.claimedPointRewards.contains(target);
        String prefix = claimed ? "§a[ ✔ ] §a" : ((current >= target) ? "§e[ ! ] §e" : "§7[ ] §f");
        return Component.literal(prefix + Math.min(current, target) + "/" + target + " " + Component.translatable("r3ct_daily.unit.points").getString() + " §8- " + color + reward);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (Services.PLATFORM.isQuestKey(event)) {
            this.onClose();
            return true;
        }

        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}