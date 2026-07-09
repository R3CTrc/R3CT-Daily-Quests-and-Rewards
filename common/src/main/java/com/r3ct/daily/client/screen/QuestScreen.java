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

import java.util.ArrayList;
import java.util.List;

public class QuestScreen extends Screen {
    private final PlayerData data;
    private final OpenQuestsPayload payload;

    private final int bookWidth = 440;
    private final int bookHeight = 340;

    private static final int STAR_COLUMN_WIDTH = 11;

    private static float animatedDaily = 0.0f;
    private static float animatedStreak = 0.0f;
    private static float animatedPoints = 0.0f;

    public QuestScreen(PlayerData data, OpenQuestsPayload payload) {
        super(Component.translatable("r3ct_daily.quests.title"));
        this.data = data;
        this.payload = payload;
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

        guiGraphics.fill(leftPos - 3, topPos - 3, leftPos + bookWidth + 3, topPos + bookHeight + 3, 0xFF3E2723);
        guiGraphics.fill(leftPos, topPos, midX - 1, topPos + bookHeight, 0xFFF5DEB3);
        guiGraphics.fill(midX + 1, topPos, leftPos + bookWidth, topPos + bookHeight, 0xFFF5DEB3);
        guiGraphics.fill(midX - 1, topPos, midX + 1, topPos + bookHeight, 0xFF8D6E63);

        int leftTextX = leftPos + 20;
        guiGraphics.text(this.font, "§0§l" + Component.translatable("r3ct_daily.quests.header.daily_quests").getString(), leftTextX, topPos + 20, 0xFF000000, false);
        guiGraphics.fill(leftTextX, topPos + 33, midX - 20, topPos + 34, 0xFF8D6E63);

        int hoveredQuestIndex = -1;
        int hoveredRerollIndex = -1;
        long time = System.currentTimeMillis();

        for (int i = 0; i < data.activeQuests.size(); i++) {
            Quest q = QuestManager.getQuestById(data.activeQuests.get(i));
            if (q == null) continue;

            int progress = data.questProgress.get(i);
            boolean done = progress >= q.requiredAmount;
            boolean claimed = data.questRewardsClaimed.size() > i && data.questRewardsClaimed.get(i);
            boolean canSubmit = !done && q.actionType.equals("SUBMIT_ITEMS") && QuestSubmitHelper.countItems(q.target) > 0;

            String locName = I18n.get(q.name);
            String locDesc = I18n.get(q.description);

            String name = (locName != null && !locName.isEmpty()) ? locName.toUpperCase() : locDesc.split(" ")[0].toUpperCase();
            int qY = topPos + 45 + (i * 55);

            if ((done && !claimed) || canSubmit) {
                int alpha = (int) (127 + 60 * Math.sin(time / 150.0));
                int rgb = (done && !claimed) ? 0x00AA00 : 0x00AAFF;
                int glowColor = (alpha << 24) | rgb;
                guiGraphics.fill(leftTextX - 2, qY - 3, midX - 12, qY + 45, glowColor);
            }

            int btnSize = 14;
            int btnX = midX - 30;
            int btnY = qY + 22;
            boolean isBtnHovered = false;

            if (!done && payload.enableQuestRerolling()) {
                isBtnHovered = mouseX >= btnX && mouseX <= btnX + btnSize && mouseY >= btnY && mouseY <= btnY + btnSize;
                if (isBtnHovered) hoveredRerollIndex = i;

                int btnBorder = isBtnHovered ? 0xFFFFFFFF : 0xFF555555;
                int btnFill = isBtnHovered ? 0xFF555555 : 0xFF333333;

                guiGraphics.fill(btnX, btnY, btnX + btnSize, btnY + btnSize, btnBorder);
                guiGraphics.fill(btnX + 1, btnY + 1, btnX + btnSize - 1, btnY + btnSize - 1, btnFill);

                String arrow = "⇄";
                int arrowColor = isBtnHovered ? 0xFFFFFFFF : 0xFFAAAAAA;
                guiGraphics.centeredText(this.font, arrow, btnX + btnSize / 2, btnY + 4, arrowColor);
            }

            if (mouseX >= leftTextX && mouseX <= midX - 10 && mouseY >= qY - 2 && mouseY <= qY + 45) {
                if (!isBtnHovered) {
                    hoveredQuestIndex = i;
                    guiGraphics.fill(leftTextX - 2, qY - 3, midX - 12, qY + 45, 0x20000000);
                }
            }

            String diffIndicator = (q.difficulty == 0) ? "§2★" : (q.difficulty == 1 ? "§6★" : "§4★");
            int titleColor = (done && !claimed) ? 0xFF005500 : (canSubmit ? 0xFF0044AA : 0xFF000000);

            String mark;
            if (!done) {
                if (canSubmit) {
                    mark = (time % 1000 < 500) ? "§f" + Component.translatable("r3ct_daily.gui.submit_items").getString() : "§b" + Component.translatable("r3ct_daily.gui.submit_items").getString();
                } else {
                    mark = "§c" + Component.translatable("r3ct_daily.quests.status.incomplete").getString();
                }
            }
            else if (claimed) mark = "§a" + Component.translatable("r3ct_daily.quests.status.claimed").getString();
            else mark = (time % 1000 < 500) ? "§e" + Component.translatable("r3ct_daily.quests.status.claim").getString() : "§6" + Component.translatable("r3ct_daily.quests.status.claim").getString();

            int markWidth = this.font.width(mark);
            int markX = midX - markWidth - 15;

            int titleStartX = leftTextX + STAR_COLUMN_WIDTH;
            int maxTitleWidth = markX - titleStartX - 5;

            String displayName = name;
            if (this.font.width(displayName) > maxTitleWidth) {
                displayName = this.font.plainSubstrByWidth(displayName, maxTitleWidth - this.font.width("...")) + "...";
            }

            guiGraphics.text(this.font, diffIndicator, leftTextX, qY, titleColor, false);
            guiGraphics.text(this.font, "§0" + displayName, titleStartX, qY, titleColor, false);
            guiGraphics.text(this.font, mark, markX, qY, 0xFF000000, false);

            int color = done ? 0xFF555555 : (q.difficulty == 0 ? 0xFF00AA00 : (q.difficulty == 1 ? 0xFFFFAA00 : 0xFFAA0000));
            String progressText = " (" + progress + "/" + q.requiredAmount + ")";
            int maxTextWidth = (midX - 45) - (leftTextX + STAR_COLUMN_WIDTH);

            String displayDesc = locDesc;
            List<FormattedCharSequence> lines = this.font.split(Component.literal(displayDesc + progressText), maxTextWidth);

            if (lines.size() > 2) {
                int charsToKeep = (int)((double)locDesc.length() * ((double)(maxTextWidth * 2 - this.font.width("..." + progressText)) / Math.max(1, this.font.width(locDesc))));
                charsToKeep = Math.max(0, Math.min(charsToKeep, locDesc.length()));

                displayDesc = locDesc.substring(0, charsToKeep) + "...";
                lines = this.font.split(Component.literal(displayDesc + progressText), maxTextWidth);

                while (lines.size() > 2 && displayDesc.length() > 4) {
                    displayDesc = displayDesc.substring(0, displayDesc.length() - 5) + "...";
                    lines = this.font.split(Component.literal(displayDesc + progressText), maxTextWidth);
                }
            }

            for (int lineIdx = 0; lineIdx < lines.size(); lineIdx++) {
                guiGraphics.text(this.font, lines.get(lineIdx), leftTextX + STAR_COLUMN_WIDTH, qY + 11 + (lineIdx * 10), color, false);
            }
        }

        int rightTextX = midX + 25;
        int barW = 160;

        guiGraphics.text(this.font, "§0§l" + Component.translatable("r3ct_daily.quests.header.progress").getString(), rightTextX, topPos + 20, 0xFF000000, false);
        guiGraphics.fill(rightTextX, topPos + 33, leftPos + bookWidth - 25, topPos + 34, 0xFF8D6E63);

        int dailyY = topPos + 80;
        int targetDaily = Math.min(data.dailyQuestsCompletedToday, 3);
        animatedDaily += (targetDaily - animatedDaily) * 0.1f;
        if (Math.abs(targetDaily - animatedDaily) < 0.05f) animatedDaily = targetDaily;

        String dailyText = "§2" + targetDaily + "§0/3";
        GuiUtils.drawRewardStyleBar(guiGraphics, this.font, rightTextX, dailyY, animatedDaily, 3, Component.translatable("r3ct_daily.quests.bar.daily").getString(), dailyText, 0xFF55FF55, barW, 1, new int[]{});

        int streakY = topPos + 155;
        int targetStreak = Math.min(data.questStreak, 7);
        animatedStreak += (targetStreak - animatedStreak) * 0.1f;
        if (Math.abs(targetStreak - animatedStreak) < 0.05f) animatedStreak = targetStreak;

        String streakColor = (targetStreak < 3) ? "§2" : (targetStreak < 7 ? "§6" : "§c");
        String streakText = streakColor + targetStreak + "§0/7";
        int streakBarColor = (targetStreak < 3) ? 0xFF006400 : (targetStreak < 7 ? 0xFFFFAA00 : 0xFFFF5555);
        GuiUtils.drawRewardStyleBar(guiGraphics, this.font, rightTextX, streakY, animatedStreak, 7, Component.translatable("r3ct_daily.quests.bar.streak").getString(), streakText, streakBarColor, barW, 1, new int[]{});

        String qMultiText = data.questStreak >= 7 ? "§6§l" + Component.translatable("r3ct_daily.quests.multiplier.active").getString() : "§0" + Component.translatable("r3ct_daily.quests.multiplier.inactive").getString();
        guiGraphics.text(this.font, qMultiText, rightTextX, streakY + 14, 0xFF000000, false);

        int lifeY = topPos + 245;
        int targetPoints = Math.min(data.totalQuestPoints, 200);
        animatedPoints += (targetPoints - animatedPoints) * 0.1f;
        if (Math.abs(targetPoints - animatedPoints) < 0.05f) animatedPoints = targetPoints;

        String ptsText = "§d" + targetPoints + "§0/200";
        GuiUtils.drawRewardStyleBar(guiGraphics, this.font, rightTextX, lifeY, animatedPoints, 200, Component.translatable("r3ct_daily.quests.bar.points").getString(), ptsText, 0xFFFF55FF, barW, 10, new int[]{50, 100, 150, 200});

        renderPointMilestones(guiGraphics, rightTextX, lifeY, barW, mouseX, mouseY);

        String arrowText = Component.translatable("r3ct_daily.quests.button.rewards_next").getString();
        int textWidth = this.font.width(arrowText);
        int arrowX = leftPos + bookWidth + 15;
        int arrowY = (this.height / 2) - 4;

        boolean isHovered = mouseX >= arrowX && mouseX <= arrowX + textWidth && mouseY >= arrowY - 2 && mouseY <= arrowY + 10;
        int arrColor = isHovered ? 0xFFFFFFFF : 0xFFAAAAAA;
        guiGraphics.text(this.font, arrowText, arrowX, arrowY, arrColor, true);

        int trophyX = leftPos + bookWidth - 18;
        int trophyY = topPos + 2;
        boolean trophyHover = mouseX >= trophyX && mouseX <= trophyX + 16 && mouseY >= trophyY && mouseY <= trophyY + 16;

        if (trophyHover) guiGraphics.fill(trophyX - 2, trophyY - 2, trophyX + 18, trophyY + 18, 0x44000000);
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

                guiGraphics.tooltip(this.font, rTooltip, mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null);
            }
        }

        if (mouseX >= rightTextX && mouseX <= rightTextX + barW && mouseY >= dailyY && mouseY <= dailyY + 8) {
            renderDailyTooltip(guiGraphics, mouseX, mouseY);
        }

        if (mouseX >= rightTextX && mouseX <= rightTextX + barW && mouseY >= streakY && mouseY <= streakY + 8) {
            renderQuestStreakTooltip(guiGraphics, mouseX, mouseY);
        }

        int[] thresholds = {50, 100, 150, 200};
        String rewLabel = Component.translatable("r3ct_daily.quests.tooltip.reward").getString();

        DailyServerConfig.MilestoneReward[] mr = {
                DailyServerConfig.milestones.point_50,
                DailyServerConfig.milestones.point_100,
                DailyServerConfig.milestones.point_150,
                DailyServerConfig.milestones.point_200
        };

        String[] tooltips = new String[4];
        String[] rewards = new String[4];
        String[] amountsStr = new String[4];

        for (int i = 0; i < 4; i++) {
            String color = mr[i].getFormattedColor();

            tooltips[i] = color + "§l" + Component.translatable("r3ct_daily.quests.tooltip.points.threshold", thresholds[i]).getString();

            ItemStack stack = QuestManager.getMilestoneRewardStack(mr[i]);
            rewards[i] = "§f" + rewLabel + " " + color + mr[i].amount + "x " + stack.getHoverName().getString();
            amountsStr[i] = color + "x" + mr[i].amount;
        }

        for (int i = 0; i < 4; i++) {
            int mX = rightTextX + (int)(thresholds[i] / 200.0 * (barW - 2));
            int totalW = 18 + this.font.width(amountsStr[i]);
            int startX = mX - totalW / 2;
            int startY = lifeY + 28;

            if (mouseX >= startX - 2 && mouseX <= startX + totalW + 2 && mouseY >= startY - 2 && mouseY <= startY + 18) {
                renderSimpleTooltip(guiGraphics, tooltips[i], rewards[i], mouseX, mouseY);
            }
        }

        if (mouseX >= rightTextX && mouseX <= rightTextX + barW && mouseY >= lifeY && mouseY <= lifeY + 8) {
            renderLifetimeTooltip(guiGraphics, mouseX, mouseY);
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

            if (hovered) {
                g.fill(startX - 2, startY - 2, startX + totalW + 2, startY + 18, 0x44000000);
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
        if (event.button() == 0) {
            float scale = DailyClientConfig.getInstance().questScreenScale;

            int mX = (int)((event.x() - this.width / 2f) / scale + this.width / 2f);
            int mY = (int)((event.y() - this.height / 2f) / scale + this.height / 2f);

            int leftPos = (this.width - bookWidth) / 2;
            int topPos = (this.height - bookHeight) / 2;
            int midX = leftPos + (bookWidth / 2);

            int trophyX = leftPos + bookWidth - 18;
            int trophyY = topPos + 2;

            if (mX >= trophyX && mX <= trophyX + 16 && mY >= trophyY && mY <= trophyY + 16) {
                if (this.minecraft != null && this.minecraft.player != null) {
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    Services.PLATFORM.sendToServer(new RequestLeaderboardPayload(0));
                    return true;
                }
            }

            int leftTextX = leftPos + 20;
            for (int i = 0; i < data.activeQuests.size(); i++) {
                int qY = topPos + 45 + (i * 55);
                Quest q = QuestManager.getQuestById(data.activeQuests.get(i));
                if (q == null) continue;

                int currentProgress = data.questProgress.get(i);
                boolean done = currentProgress >= q.requiredAmount;
                boolean claimed = data.questRewardsClaimed.size() > i && data.questRewardsClaimed.get(i);

                int btnSize = 14;
                int btnX = midX - 30;
                int btnY = qY + 22;

                if (!done && payload.enableQuestRerolling() && mX >= btnX && mX <= btnX + btnSize && mY >= btnY && mY <= btnY + btnSize) {
                    if (this.minecraft != null && this.minecraft.player != null) {
                        Services.PLATFORM.sendToServer(new RerollQuestPayload(i));
                        return true;
                    }
                }

                if (mX >= leftTextX && mX <= midX - 10 && mY >= qY - 2 && mY <= qY + 45) {
                    if (done && !claimed) {
                        if (this.minecraft != null && this.minecraft.player != null) {
                            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                            this.minecraft.player.connection.sendCommand("daily claimquest " + i);
                            return true;
                        }
                    } else if (!done && q.actionType.equals("SUBMIT_ITEMS")) {
                        if (this.minecraft != null && this.minecraft.player != null) {
                            if (QuestSubmitHelper.countItems(q.target) > 0) {
                                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                                QuestSubmitHelper.handleQuestSubmitClick(this, q, i, currentProgress);
                                return true;
                            }
                        }
                    }
                }
            }

            int rightTextX = midX + 25;
            int barW = 160;
            int lifeY = topPos + 245;
            int[] thresholds = {50, 100, 150, 200};

            DailyServerConfig.MilestoneReward[] mr = {
                    DailyServerConfig.milestones.point_50,
                    DailyServerConfig.milestones.point_100,
                    DailyServerConfig.milestones.point_150,
                    DailyServerConfig.milestones.point_200
            };

            for (int i = 0; i < 4; i++) {
                int t = thresholds[i];
                String amountsStr = mr[i].getFormattedColor() + "x" + mr[i].amount;

                int iconX = rightTextX + (int)(t / 200.0 * (barW - 2));
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

        if (data.questStreak >= 7) {
            tooltip.add(ClientTooltipComponent.create(Component.literal(Component.translatable("r3ct_daily.quests.tooltip.streak.multi_active").getString()).getVisualOrderText()));
            tooltip.add(ClientTooltipComponent.create(Component.literal("§7" + Component.translatable("r3ct_daily.quests.tooltip.streak.multi_desc").getString()).getVisualOrderText()));
        } else {
            tooltip.add(ClientTooltipComponent.create(Component.literal("§f" + Component.translatable("r3ct_daily.quests.tooltip.streak.req1").getString()).getVisualOrderText()));
            tooltip.add(ClientTooltipComponent.create(Component.literal("§f" + Component.translatable("r3ct_daily.quests.tooltip.streak.req2").getString()).getVisualOrderText()));
        }

        tooltip.add(ClientTooltipComponent.create(Component.literal("").getVisualOrderText()));
        tooltip.add(ClientTooltipComponent.create(Component.literal("§b" + Component.translatable("r3ct_daily.quests.tooltip.streak.freeze_title").getString()).getVisualOrderText()));
        tooltip.add(ClientTooltipComponent.create(Component.literal("").getVisualOrderText()));
        int reqDays = payload.perfectDaysForShield();
        tooltip.add(ClientTooltipComponent.create(Component.literal("§f" + Component.translatable("r3ct_daily.quests.tooltip.streak.freeze_desc1", reqDays).getString()).getVisualOrderText()));
        tooltip.add(ClientTooltipComponent.create(Component.literal("§f" + Component.translatable("r3ct_daily.quests.tooltip.streak.freeze_desc2").getString()).getVisualOrderText()));
        tooltip.add(ClientTooltipComponent.create(Component.literal("").getVisualOrderText()));

        int maxQuestShields = payload.maxStoredQuestShields();
        tooltip.add(ClientTooltipComponent.create(Component.literal("§f" + Component.translatable("r3ct_daily.quests.tooltip.streak.freezes").getString() + " §b" + data.availableFreezes + "§f/" + maxQuestShields).getVisualOrderText()));

        tooltip.add(ClientTooltipComponent.create(Component.literal("§f" + Component.translatable("r3ct_daily.quests.tooltip.streak.progress").getString() + " §b" + data.perfectDaysCount + "§f/" + reqDays).getVisualOrderText()));

        guiGraphics.tooltip(this.font, tooltip, mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null);
    }

    private void renderSimpleTooltip(GuiGraphicsExtractor guiGraphics, String title, String info, int mouseX, int mouseY) {
        List<ClientTooltipComponent> tooltip = new ArrayList<>();
        tooltip.add(ClientTooltipComponent.create(Component.literal(title).getVisualOrderText()));
        tooltip.add(ClientTooltipComponent.create(Component.literal("§8----------------").getVisualOrderText()));
        tooltip.add(ClientTooltipComponent.create(Component.literal(info).getVisualOrderText()));
        guiGraphics.tooltip(this.font, tooltip, mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null);
    }

    private void renderQuestTooltip(GuiGraphicsExtractor guiGraphics, int index, int mouseX, int mouseY) {
        Quest q = QuestManager.getQuestById(data.activeQuests.get(index));
        if (q == null) return;

        int multi = (data.questStreak >= 7) ? 2 : 1;

        int baseXp = (q.difficulty == 0) ? payload.xpPerQuestEasy() :
                (q.difficulty == 1) ? payload.xpPerQuestMedium() :
                        payload.xpPerQuestHard();
        int xpReward = baseXp * multi;

        int itemAmount = q.rewardAmount * multi;

        List<ClientTooltipComponent> tooltip = new ArrayList<>();

        String questTitle = Component.translatable("r3ct_daily.quests.tooltip.quest.title", (index + 1)).getString();
        tooltip.add(ClientTooltipComponent.create(Component.literal("§l" + questTitle).getVisualOrderText()));

        String diffName = (q.difficulty == 0) ? "§2" + Component.translatable("r3ct_daily.quests.tooltip.quest.diff.0").getString() : ((q.difficulty == 1) ? "§6" + Component.translatable("r3ct_daily.quests.tooltip.quest.diff.1").getString() : "§4" + Component.translatable("r3ct_daily.quests.tooltip.quest.diff.2").getString());
        tooltip.add(ClientTooltipComponent.create(Component.literal(Component.translatable("r3ct_daily.quests.tooltip.quest.diff_label").getString() + " " + diffName).getVisualOrderText()));

        tooltip.add(ClientTooltipComponent.create(Component.literal("§8----------------").getVisualOrderText()));

        String locDesc = I18n.get(q.description);

        String descColor = (q.difficulty == 0) ? "§2" : (q.difficulty == 1 ? "§6" : "§4");
        String descPrefix = "§f" + Component.translatable("r3ct_daily.quests.tooltip.quest.desc").getString() + " " + descColor;

        List<FormattedCharSequence> descLines = this.font.split(Component.literal(descPrefix + locDesc), 200);
        for (FormattedCharSequence seq : descLines) {
            tooltip.add(ClientTooltipComponent.create(seq));
        }

        tooltip.add(ClientTooltipComponent.create(Component.literal("§f" + Component.translatable("r3ct_daily.quests.tooltip.quest.points").getString() + " §d+" + q.points).getVisualOrderText()));
        tooltip.add(ClientTooltipComponent.create(Component.literal("§f" + Component.translatable("r3ct_daily.quests.tooltip.quest.xp").getString() + " §e+" + xpReward + " §e" + Component.translatable("r3ct_daily.unit.xp").getString()).getVisualOrderText()));
        tooltip.add(ClientTooltipComponent.create(Component.literal("§f" + Component.translatable("r3ct_daily.quests.tooltip.reward").getString() + " §b" + itemAmount + "§bx §b" + q.getItemReward().getHoverName().getString()).getVisualOrderText()));

        guiGraphics.tooltip(this.font, tooltip, mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null);
    }

    private void renderDailyTooltip(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        List<ClientTooltipComponent> tooltip = new ArrayList<>();
        tooltip.add(ClientTooltipComponent.create(Component.literal("§f§l" + Component.translatable("r3ct_daily.quests.tooltip.daily.title").getString()).getVisualOrderText()));
        tooltip.add(ClientTooltipComponent.create(Component.literal("§8----------------").getVisualOrderText()));
        tooltip.add(ClientTooltipComponent.create(Component.literal("§f" + Component.translatable("r3ct_daily.quests.tooltip.daily.desc1").getString()).getVisualOrderText()));
        tooltip.add(ClientTooltipComponent.create(Component.literal("§f" + Component.translatable("r3ct_daily.quests.tooltip.daily.desc2").getString()).getVisualOrderText()));
        tooltip.add(ClientTooltipComponent.create(Component.literal("").getVisualOrderText()));

        int dailyXp = payload.xpDailyReward();
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

        guiGraphics.tooltip(this.font, tooltip, mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null);
    }

    private void renderLifetimeTooltip(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        List<ClientTooltipComponent> tooltip = new ArrayList<>();
        tooltip.add(ClientTooltipComponent.create(Component.literal("§d§l" + Component.translatable("r3ct_daily.quests.tooltip.lifetime.title").getString()).getVisualOrderText()));
        tooltip.add(ClientTooltipComponent.create(Component.literal("§8----------------").getVisualOrderText()));

        DailyServerConfig.MilestoneReward[] mr = {
                DailyServerConfig.milestones.point_50,
                DailyServerConfig.milestones.point_100,
                DailyServerConfig.milestones.point_150,
                DailyServerConfig.milestones.point_200
        };
        int[] thresholds = {50, 100, 150, 200};

        for (int i = 0; i < 4; i++) {
            ItemStack stack = QuestManager.getMilestoneRewardStack(mr[i]);
            String label = mr[i].amount + "x " + stack.getHoverName().getString();

            tooltip.add(ClientTooltipComponent.create(
                    getLifetimeTooltipLine(data.totalQuestPoints, thresholds[i], label, mr[i].getFormattedColor()).getVisualOrderText()
            ));
        }

        guiGraphics.tooltip(this.font, tooltip, mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null);
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