package com.r3ct.quests.screen;

import com.r3ct.quests.data.PlayerData;
import com.r3ct.quests.logic.Quest;
import com.r3ct.quests.logic.QuestManager;
import com.r3ct.quests.config.ConfigLoader;
import com.r3ct.quests.network.RequestLeaderboardPayload;
import com.r3ct.quests.network.RerollQuestPayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

public class QuestScreen extends Screen {
    private final PlayerData data;

    private final int bookWidth = 440;
    private final int bookHeight = 340;

    private static float animatedDaily = 0.0f;
    private static float animatedStreak = 0.0f;
    private static float animatedPoints = 0.0f;

    public QuestScreen(PlayerData data) {
        super(Component.translatable("r3ct.quests.title"));
        this.data = data;
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.extractTransparentBackground(guiGraphics);

        float scale = ConfigLoader.mechanics != null ? com.r3ct.quests.config.R3CTQuestsConfig.getInstance().questScreenScale : 1.0f;

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
        guiGraphics.text(this.font, "§0§l" + Component.translatable("r3ct.quests.header.daily_quests").getString(), leftTextX, topPos + 20, 0xFF000000, false);
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

            String locName = net.minecraft.client.resources.language.I18n.get(q.name);
            String locDesc = net.minecraft.client.resources.language.I18n.get(q.description);

            String name = (locName != null && !locName.isEmpty()) ? locName.toUpperCase() : locDesc.split(" ")[0].toUpperCase();
            int qY = topPos + 45 + (i * 55);

            if (done && !claimed) {
                int alpha = (int) (127 + 60 * Math.sin(time / 150.0));
                int glowColor = (alpha << 24) | 0x00AA00;
                guiGraphics.fill(leftTextX - 2, qY - 3, midX - 12, qY + 45, glowColor);
            }

            int btnSize = 14;
            int btnX = midX - 30;
            int btnY = qY + 22;
            boolean isBtnHovered = false;

            if (!done && ConfigLoader.mechanics.quests.enableQuestRerolling) {
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

            String diffIndicator = (q.difficulty == 0) ? "§2★ " : (q.difficulty == 1 ? "§6★ " : "§4★ ");
            int titleColor = (done && !claimed) ? 0xFF005500 : 0xFF000000;
            guiGraphics.text(this.font, diffIndicator + " §0" + name, leftTextX, qY, titleColor, false);

            int color = done ? 0xFF555555 : (q.difficulty == 0 ? 0xFF00AA00 : (q.difficulty == 1 ? 0xFFFFAA00 : 0xFFAA0000));
            String desc = locDesc + " (" + progress + "/" + q.requiredAmount + ")";

            int maxTextWidth = (midX - 45) - (leftTextX + 11);
            List<net.minecraft.util.FormattedCharSequence> lines = this.font.split(Component.literal(desc), maxTextWidth);

            for (int lineIdx = 0; lineIdx < lines.size(); lineIdx++) {
                guiGraphics.text(this.font, lines.get(lineIdx), leftTextX + 11, qY + 11 + (lineIdx * 10), color, false);
            }

            String mark;
            if (!done) mark = "§c" + Component.translatable("r3ct.quests.status.incomplete").getString();
            else if (claimed) mark = "§a" + Component.translatable("r3ct.quests.status.claimed").getString();
            else mark = (time % 1000 < 500) ? "§e" + Component.translatable("r3ct.quests.status.claim").getString() : "§6" + Component.translatable("r3ct.quests.status.claim").getString();

            guiGraphics.text(this.font, mark, midX - (this.font.width(mark) + 15), qY + 5, 0xFF000000, false);
        }

        int rightTextX = midX + 25;
        int barW = 160;

        guiGraphics.text(this.font, "§0§l" + Component.translatable("r3ct.quests.header.progress").getString(), rightTextX, topPos + 20, 0xFF000000, false);
        guiGraphics.fill(rightTextX, topPos + 33, leftPos + bookWidth - 25, topPos + 34, 0xFF8D6E63);

        int dailyY = topPos + 80;
        int targetDaily = Math.min(data.dailyQuestsCompletedToday, 3);
        animatedDaily += (targetDaily - animatedDaily) * 0.1f;
        if (Math.abs(targetDaily - animatedDaily) < 0.05f) animatedDaily = targetDaily;

        String dailyText = "§2" + targetDaily + "§0/3";
        drawRewardStyleBar(guiGraphics, rightTextX, dailyY, animatedDaily, 3, Component.translatable("r3ct.quests.bar.daily").getString(), dailyText, 0xFF55FF55, barW, 1, new int[]{});

        int streakY = topPos + 155;
        int targetStreak = Math.min(data.questStreak, 7);
        animatedStreak += (targetStreak - animatedStreak) * 0.1f;
        if (Math.abs(targetStreak - animatedStreak) < 0.05f) animatedStreak = targetStreak;

        String streakColor = (targetStreak < 3) ? "§2" : (targetStreak < 7 ? "§6" : "§c");
        String streakText = streakColor + targetStreak + "§0/7";
        int streakBarColor = (targetStreak < 3) ? 0xFF006400 : (targetStreak < 7 ? 0xFFFFAA00 : 0xFFFF5555);
        drawRewardStyleBar(guiGraphics, rightTextX, streakY, animatedStreak, 7, Component.translatable("r3ct.quests.bar.streak").getString(), streakText, streakBarColor, barW, 1, new int[]{});

        String qMultiText = data.questStreak >= 7 ? "§6§l" + Component.translatable("r3ct.quests.multiplier.active").getString() : "§0" + Component.translatable("r3ct.quests.multiplier.inactive").getString();
        guiGraphics.text(this.font, qMultiText, rightTextX, streakY + 14, 0xFF000000, false);

        int lifeY = topPos + 245;
        int targetPoints = Math.min(data.totalQuestPoints, 200);
        animatedPoints += (targetPoints - animatedPoints) * 0.1f;
        if (Math.abs(targetPoints - animatedPoints) < 0.05f) animatedPoints = targetPoints;

        String ptsText = "§d" + targetPoints + "§0/200";
        drawRewardStyleBar(guiGraphics, rightTextX, lifeY, animatedPoints, 200, Component.translatable("r3ct.quests.bar.points").getString(), ptsText, 0xFFFF55FF, barW, 10, new int[]{50, 100, 150, 200});

        renderPointMilestones(guiGraphics, rightTextX, lifeY, barW, mouseX, mouseY);

        String arrowText = Component.translatable("r3ct.quests.button.rewards_next").getString();
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

        String backText = Component.translatable("r3ct.quests.button.close").getString();
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
                int cost = (rq.difficulty == 0) ? ConfigLoader.mechanics.quests.rerollCostEasy :
                        (rq.difficulty == 1) ? ConfigLoader.mechanics.quests.rerollCostMedium :
                                ConfigLoader.mechanics.quests.rerollCostHard;

                List<net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent> rTooltip = new ArrayList<>();
                rTooltip.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(Component.literal("§e§l" + Component.translatable("r3ct.quests.tooltip.reroll.title").getString()).getVisualOrderText()));
                rTooltip.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(Component.literal("§8----------------").getVisualOrderText()));
                rTooltip.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(Component.literal("§f" + Component.translatable("r3ct.quests.tooltip.reroll.desc1").getString()).getVisualOrderText()));
                rTooltip.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(Component.literal("§f" + Component.translatable("r3ct.quests.tooltip.reroll.desc2").getString()).getVisualOrderText()));
                rTooltip.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(Component.literal("").getVisualOrderText()));

                String costColor = (data.totalQuestPoints >= cost) ? "§a" : "§c";
                rTooltip.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(Component.literal("§f" + Component.translatable("r3ct.quests.tooltip.reroll.cost").getString() + " " + costColor + cost + " " + Component.translatable("r3ct.unit.points").getString()).getVisualOrderText()));

                guiGraphics.tooltip(this.font, rTooltip, mouseX, mouseY, net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner.INSTANCE, null);
            }
        }

        if (mouseX >= rightTextX && mouseX <= rightTextX + barW && mouseY >= dailyY && mouseY <= dailyY + 8) {
            renderDailyTooltip(guiGraphics, mouseX, mouseY);
        }

        if (mouseX >= rightTextX && mouseX <= rightTextX + barW && mouseY >= streakY && mouseY <= streakY + 8) {
            renderQuestStreakTooltip(guiGraphics, mouseX, mouseY);
        }

        int[] thresholds = {50, 100, 150, 200};
        String rewLabel = Component.translatable("r3ct.quests.tooltip.reward").getString();

        String[] tooltips = {
                "§d§l" + Component.translatable("r3ct.quests.tooltip.points.threshold", 50).getString(),
                "§a§l" + Component.translatable("r3ct.quests.tooltip.points.threshold", 100).getString(),
                "§b§l" + Component.translatable("r3ct.quests.tooltip.points.threshold", 150).getString(),
                "§c§l" + Component.translatable("r3ct.quests.tooltip.points.threshold", 200).getString()
        };

        String[] rewards = {
                "§f" + rewLabel + " §d32x " + Component.translatable("r3ct.quests.item.amethyst").getString(),
                "§f" + rewLabel + " §a16x " + Component.translatable("r3ct.quests.item.emerald").getString(),
                "§f" + rewLabel + " §b8x " + Component.translatable("r3ct.quests.item.diamond").getString(),
                "§f" + rewLabel + " §c4x " + Component.translatable("r3ct.quests.item.netherite").getString()
        };

        String[] amountsStr = {"§dx32", "§ax16", "§bx8", "§cx4"};

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
            List<net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent> tTooltip = new ArrayList<>();
            tTooltip.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(Component.literal("§6§l" + Component.translatable("r3ct.quests.tooltip.leaderboard.title").getString()).getVisualOrderText()));
            tTooltip.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(Component.literal("§8----------------").getVisualOrderText()));
            tTooltip.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(Component.literal("§f" + Component.translatable("r3ct.quests.tooltip.leaderboard.desc").getString()).getVisualOrderText()));
            guiGraphics.tooltip(this.font, tTooltip, mouseX, mouseY, net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner.INSTANCE, null);
        }

        guiGraphics.pose().popMatrix();
    }

    private void renderPointMilestones(GuiGraphicsExtractor g, int x, int y, int bWidth, int mouseX, int mouseY) {
        int[] thresholds = {50, 100, 150, 200};
        ItemStack[] icons = {new ItemStack(Items.AMETHYST_SHARD), new ItemStack(Items.EMERALD), new ItemStack(Items.DIAMOND), new ItemStack(Items.NETHERITE_SCRAP)};
        String[] amounts = {"§dx32", "§ax16", "§bx8", "§cx4"};
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
            float scale = com.r3ct.quests.config.R3CTQuestsConfig.getInstance().questScreenScale;

            int mX = (int)((event.x() - this.width / 2f) / scale + this.width / 2f);
            int mY = (int)((event.y() - this.height / 2f) / scale + this.height / 2f);

            int leftPos = (this.width - bookWidth) / 2;
            int topPos = (this.height - bookHeight) / 2;
            int midX = leftPos + (bookWidth / 2);

            int trophyX = leftPos + bookWidth - 18;
            int trophyY = topPos + 2;

            if (mX >= trophyX && mX <= trophyX + 16 && mY >= trophyY && mY <= trophyY + 16) {
                if (this.minecraft != null && this.minecraft.player != null) {
                    this.minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    com.r3ct.quests.platform.Services.PLATFORM.sendToServer(new RequestLeaderboardPayload(0));
                    return true;
                }
            }

            int leftTextX = leftPos + 20;
            for (int i = 0; i < data.activeQuests.size(); i++) {
                int qY = topPos + 45 + (i * 55);
                Quest q = QuestManager.getQuestById(data.activeQuests.get(i));
                if (q == null) continue;

                boolean done = data.questProgress.get(i) >= q.requiredAmount;
                boolean claimed = data.questRewardsClaimed.size() > i && data.questRewardsClaimed.get(i);

                int btnSize = 14;
                int btnX = midX - 30;
                int btnY = qY + 22;

                if (!done && ConfigLoader.mechanics.quests.enableQuestRerolling && mX >= btnX && mX <= btnX + btnSize && mY >= btnY && mY <= btnY + btnSize) {
                    if (this.minecraft != null && this.minecraft.player != null) {
                        com.r3ct.quests.platform.Services.PLATFORM.sendToServer(new RerollQuestPayload(i));
                        return true;
                    }
                }

                if (mX >= leftTextX && mX <= midX - 10 && mY >= qY - 2 && mY <= qY + 45) {
                    if (done && !claimed) {
                        if (this.minecraft != null && this.minecraft.player != null) {
                            this.minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                            this.minecraft.player.connection.sendCommand("daily claimquest " + i);
                            return true;
                        }
                    }
                }
            }

            int rightTextX = midX + 25;
            int barW = 160;
            int lifeY = topPos + 245;
            int[] thresholds = {50, 100, 150, 200};
            String[] amountsStr = {"§dx32", "§ax16", "§bx8", "§cx4"};

            for (int i = 0; i < 4; i++) {
                int t = thresholds[i];
                int iconX = rightTextX + (int)(t / 200.0 * (barW - 2));
                int totalW = 18 + this.font.width(amountsStr[i]);
                int startX = iconX - totalW / 2;
                int startY = lifeY + 28;

                if (mX >= startX - 2 && mX <= startX + totalW + 2 && mY >= startY - 2 && mY <= startY + 18) {
                    boolean canClaim = data.totalQuestPoints >= t && !data.claimedPointRewards.contains(t);
                    if (canClaim) {
                        if (this.minecraft != null && this.minecraft.player != null) {
                            this.minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                            this.minecraft.player.connection.sendCommand("daily claimpoints " + t);
                            return true;
                        }
                    }
                }
            }

            String arrowText = Component.translatable("r3ct.quests.button.rewards_next").getString();
            int textWidth = this.font.width(arrowText);
            int arrowX = leftPos + bookWidth + 15;
            int arrowY = (this.height / 2) - 4;

            if (mX >= arrowX && mX <= arrowX + textWidth && mY >= arrowY - 2 && mY <= arrowY + 10) {
                if (this.minecraft != null && this.minecraft.player != null) {
                    this.minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN, 1.0F));
                    this.minecraft.player.connection.sendCommand("daily rewards");
                    return true;
                }
            }

            String backText = Component.translatable("r3ct.quests.button.close").getString();
            int backWidth = this.font.width(backText);
            int backX = midX - (backWidth / 2);
            int backY = topPos + bookHeight + 14;

            if (mX >= backX - 2 && mX <= backX + backWidth + 2 && mY >= backY - 2 && mY <= backY + 10) {
                if (this.minecraft != null && this.minecraft.player != null) {
                    this.minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    this.onClose();
                    return true;
                }
            }
        }
        return false;
    }

    private void renderQuestStreakTooltip(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        List<net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent> tooltip = new ArrayList<>();
        tooltip.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(Component.literal("§6§l" + Component.translatable("r3ct.quests.tooltip.streak.title").getString()).getVisualOrderText()));
        tooltip.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(Component.literal("§8----------------").getVisualOrderText()));

        if (data.questStreak >= 7) {
            tooltip.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(Component.literal(Component.translatable("r3ct.quests.tooltip.streak.multi_active").getString()).getVisualOrderText()));
            tooltip.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(Component.literal("§7" + Component.translatable("r3ct.quests.tooltip.streak.multi_desc").getString()).getVisualOrderText()));
        } else {
            tooltip.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(Component.literal("§f" + Component.translatable("r3ct.quests.tooltip.streak.req1").getString()).getVisualOrderText()));
            tooltip.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(Component.literal("§f" + Component.translatable("r3ct.quests.tooltip.streak.req2").getString()).getVisualOrderText()));
        }

        tooltip.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(Component.literal("").getVisualOrderText()));
        tooltip.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(Component.literal("§b" + Component.translatable("r3ct.quests.tooltip.streak.freeze_title").getString()).getVisualOrderText()));
        tooltip.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(Component.literal("").getVisualOrderText()));
        int reqDays = ConfigLoader.mechanics.streaks.perfectDaysForShield;
        tooltip.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(Component.literal("§f" + Component.translatable("r3ct.quests.tooltip.streak.freeze_desc1", reqDays).getString()).getVisualOrderText()));
        tooltip.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(Component.literal("§f" + Component.translatable("r3ct.quests.tooltip.streak.freeze_desc2").getString()).getVisualOrderText()));
        tooltip.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(Component.literal("").getVisualOrderText()));

        int maxQuestShields = ConfigLoader.mechanics.streaks.maxStoredQuestShields;
        tooltip.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(Component.literal("§f" + Component.translatable("r3ct.quests.tooltip.streak.freezes").getString() + " §b" + data.availableFreezes + "§f/" + maxQuestShields).getVisualOrderText()));

        tooltip.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(Component.literal("§f" + Component.translatable("r3ct.quests.tooltip.streak.progress").getString() + " §b" + data.perfectDaysCount + "§f/" + reqDays).getVisualOrderText()));

        guiGraphics.tooltip(this.font, tooltip, mouseX, mouseY, net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner.INSTANCE, null);
    }

    private void renderSimpleTooltip(GuiGraphicsExtractor guiGraphics, String title, String info, int mouseX, int mouseY) {
        List<net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent> tooltip = new ArrayList<>();
        tooltip.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(Component.literal(title).getVisualOrderText()));
        tooltip.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(Component.literal("§8----------------").getVisualOrderText()));
        tooltip.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(Component.literal(info).getVisualOrderText()));
        guiGraphics.tooltip(this.font, tooltip, mouseX, mouseY, net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner.INSTANCE, null);
    }

    private void renderQuestTooltip(GuiGraphicsExtractor guiGraphics, int index, int mouseX, int mouseY) {
        Quest q = QuestManager.getQuestById(data.activeQuests.get(index));
        if (q == null) return;

        int multi = (data.questStreak >= 7) ? 2 : 1;

        int baseXp = (q.difficulty == 0) ? ConfigLoader.mechanics.quests.xpPerQuestEasy :
                (q.difficulty == 1) ? ConfigLoader.mechanics.quests.xpPerQuestMedium :
                        ConfigLoader.mechanics.quests.xpPerQuestHard;
        int xpReward = baseXp * multi;

        int itemAmount = q.rewardAmount * multi;

        List<net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent> tooltip = new ArrayList<>();

        String questTitle = Component.translatable("r3ct.quests.tooltip.quest.title", (index + 1)).getString();
        tooltip.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(Component.literal("§l" + questTitle).getVisualOrderText()));

        String diffName = (q.difficulty == 0) ? "§2" + Component.translatable("r3ct.quests.tooltip.quest.diff.0").getString() : ((q.difficulty == 1) ? "§6" + Component.translatable("r3ct.quests.tooltip.quest.diff.1").getString() : "§4" + Component.translatable("r3ct.quests.tooltip.quest.diff.2").getString());
        tooltip.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(Component.literal(Component.translatable("r3ct.quests.tooltip.quest.diff_label").getString() + " " + diffName).getVisualOrderText()));

        tooltip.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(Component.literal("§8----------------").getVisualOrderText()));
        String locDesc = net.minecraft.client.resources.language.I18n.get(q.description);
        tooltip.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(Component.literal("§f" + Component.translatable("r3ct.quests.tooltip.quest.desc").getString() + " §7" + locDesc).getVisualOrderText()));
        tooltip.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(Component.literal("§f" + Component.translatable("r3ct.quests.tooltip.quest.points").getString() + " §d+" + q.points).getVisualOrderText()));
        tooltip.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(Component.literal("§f" + Component.translatable("r3ct.quests.tooltip.quest.xp").getString() + " §e+" + xpReward + " §e" + Component.translatable("r3ct.unit.xp").getString()).getVisualOrderText()));
        tooltip.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(Component.literal("§f" + Component.translatable("r3ct.quests.tooltip.reward").getString() + " §a" + itemAmount + "§ax §a" + q.getItemReward().getHoverName().getString()).getVisualOrderText()));

        guiGraphics.tooltip(this.font, tooltip, mouseX, mouseY, net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner.INSTANCE, null);
    }

    private void renderDailyTooltip(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        List<net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent> tooltip = new ArrayList<>();
        tooltip.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(Component.literal("§f§l" + Component.translatable("r3ct.quests.tooltip.daily.title").getString()).getVisualOrderText()));
        tooltip.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(Component.literal("§8----------------").getVisualOrderText()));
        tooltip.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(Component.literal("§f" + Component.translatable("r3ct.quests.tooltip.daily.desc1").getString()).getVisualOrderText()));
        tooltip.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(Component.literal("§f" + Component.translatable("r3ct.quests.tooltip.daily.desc2").getString()).getVisualOrderText()));
        tooltip.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(Component.literal("").getVisualOrderText()));

        int dailyXp = ConfigLoader.mechanics.quests.xpDailyReward;
        tooltip.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(Component.literal("§f" + Component.translatable("r3ct.quests.tooltip.quest.xp").getString() + " §e+" + dailyXp + " §e" + Component.translatable("r3ct.unit.xp").getString()).getVisualOrderText()));

        tooltip.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(Component.literal("§f" + Component.translatable("r3ct.quests.tooltip.daily.list_title").getString()).getVisualOrderText()));

        tooltip.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(Component.literal("§8- " + Component.translatable("r3ct.quests.item.coal").getString()).getVisualOrderText()));
        tooltip.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(Component.literal("§9- " + Component.translatable("r3ct.quests.item.lapis").getString()).getVisualOrderText()));
        tooltip.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(Component.literal("§f- " + Component.translatable("r3ct.quests.item.iron").getString()).getVisualOrderText()));
        tooltip.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(Component.literal("§e- " + Component.translatable("r3ct.quests.item.gold").getString()).getVisualOrderText()));
        tooltip.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(Component.literal("§d- " + Component.translatable("r3ct.quests.item.amethyst").getString()).getVisualOrderText()));
        tooltip.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(Component.literal("§a- " + Component.translatable("r3ct.quests.item.emerald").getString()).getVisualOrderText()));
        tooltip.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(Component.literal("§b- " + Component.translatable("r3ct.quests.item.diamond").getString()).getVisualOrderText()));

        guiGraphics.tooltip(this.font, tooltip, mouseX, mouseY, net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner.INSTANCE, null);
    }

    private void renderLifetimeTooltip(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        List<net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent> tooltip = new ArrayList<>();
        tooltip.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(Component.literal("§d§l" + Component.translatable("r3ct.quests.tooltip.lifetime.title").getString()).getVisualOrderText()));
        tooltip.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(Component.literal("§8----------------").getVisualOrderText()));

        tooltip.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(getLifetimeTooltipLine(data.totalQuestPoints, 50, "32x " + Component.translatable("r3ct.quests.item.amethyst").getString(), "§d").getVisualOrderText()));
        tooltip.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(getLifetimeTooltipLine(data.totalQuestPoints, 100, "16x " + Component.translatable("r3ct.quests.item.emerald").getString(), "§a").getVisualOrderText()));
        tooltip.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(getLifetimeTooltipLine(data.totalQuestPoints, 150, "8x " + Component.translatable("r3ct.quests.item.diamond").getString(), "§b").getVisualOrderText()));
        tooltip.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(getLifetimeTooltipLine(data.totalQuestPoints, 200, "4x " + Component.translatable("r3ct.quests.item.netherite").getString(), "§c").getVisualOrderText()));

        guiGraphics.tooltip(this.font, tooltip, mouseX, mouseY, net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner.INSTANCE, null);
    }

    private Component getLifetimeTooltipLine(int current, int target, String reward, String color) {
        boolean claimed = data.claimedPointRewards.contains(target);
        String prefix = claimed ? "§a[ ✔ ] §a" : ((current >= target) ? "§e[ ! ] §e" : "§7[ ] §f");
        return Component.literal(prefix + Math.min(current, target) + "/" + target + " " + Component.translatable("r3ct.unit.points").getString() + " §8- " + color + reward);
    }

    private void drawRewardStyleBar(GuiGraphicsExtractor g, int x, int y, float val, int max, String label, String valueText, int color, int bWidth, int tickStep, int[] labelsToDraw) {
        g.text(this.font, "§0" + label + ": " + valueText, x, y - 12, 0xFF000000, false);
        g.fill(x, y, x + bWidth, y + 8, 0xFF373737);
        int w = (int)((Math.min(val, max) / (float)max) * (bWidth - 2));
        if (w > 0) g.fill(x + 1, y + 1, x + 1 + w, y + 7, color | 0xFF000000);

        for (int p = tickStep; p < max; p += tickStep) {
            int tickX = x + (int)((p / (float)max) * (bWidth - 2));
            g.fill(tickX, y, tickX + 1, y + 8, 0xFF000000);
        }

        for (int p : labelsToDraw) {
            int tickX = x + (int)((p / (float)max) * (bWidth - 2));
            g.fill(tickX, y - 2, tickX + 1, y + 10, 0xFFFFFFFF);
            String pStr = String.valueOf(p);
            g.text(this.font, "§0" + pStr, tickX - (this.font.width(pStr) / 2), y + 13, 0xFF000000, false);
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (com.r3ct.quests.platform.Services.PLATFORM.isQuestKey(event)) {
            this.onClose();
            return true;
        }

        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}