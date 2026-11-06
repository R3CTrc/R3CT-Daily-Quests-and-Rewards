package com.r3ct.daily.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.r3ct.daily.config.DailyClientConfig;
import com.r3ct.daily.data.TopEntry;
import com.r3ct.daily.network.RequestLeaderboardPayload;
import com.r3ct.daily.platform.Services;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import org.jspecify.annotations.NonNull;
import org.lwjgl.sdl.SDLMouse;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardScreen extends Screen {
    private static final int MAX_TOP_PLAYERS = 100;
    private static final int VISIBLE_ROWS = 10;
    private static final int ROW_STEP = 18;
    private static final int ROW_WIDTH = 166;

    private final int boardType;
    private final List<TopEntry> leftList;
    private final List<TopEntry> rightList;

    private final int boardWidth = 412;
    private final int boardHeight = 268;
    private TopEntry hoveredEntry = null;

    private int leftScrollOffset = 0;
    private int rightScrollOffset = 0;
    private int draggingScrollbar = 0;

    public LeaderboardScreen(int boardType, List<TopEntry> leftList, List<TopEntry> rightList) {
        super(Component.translatable("r3ct_daily.leaderboard.title"));
        this.boardType = boardType;
        this.leftList = leftList;
        this.rightList = rightList;
    }

    private int getScore(TopEntry entry, boolean isLeftColumn) {
        if (this.boardType == 0 && isLeftColumn) return entry.totalQuests();
        if (this.boardType == 0) return entry.maxQuestStreak();
        if (this.boardType == 1 && isLeftColumn) return entry.totalRewards();
        return entry.maxRewardStreak();
    }

    private int getTop100Count(List<TopEntry> rawList, boolean isLeftColumn) {
        int count = 0;
        for (int i = 0; i < rawList.size() && count < MAX_TOP_PLAYERS; i++) {
            if (getScore(rawList.get(i), isLeftColumn) > 0) {
                count++;
            }
        }
        return count;
    }

    private String getClientPlayerName() {
        if (this.minecraft != null && this.minecraft.player != null) {
            return this.minecraft.player.getGameProfile().name();
        }
        return "";
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

        float scale = DailyClientConfig.getInstance().leaderboardScreenScale;

        mouseX = (int)((mouseX - this.width / 2f) / scale + this.width / 2f);
        mouseY = (int)((mouseY - this.height / 2f) / scale + this.height / 2f);

        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(this.width / 2f, this.height / 2f);
        guiGraphics.pose().scale(scale, scale);
        guiGraphics.pose().translate(-this.width / 2f, -this.height / 2f);

        int leftPos = (this.width - boardWidth) / 2;
        int topPos = (this.height - boardHeight) / 2;
        int midX = leftPos + (boardWidth / 2);

        drawHallOfFameBoard(guiGraphics, leftPos, topPos, midX);

        String title = "§6§l" + Component.translatable(boardType == 0 ? "r3ct_daily.leaderboard.title.quests" : "r3ct_daily.leaderboard.title.rewards").getString();
        guiGraphics.centeredText(this.font, title, midX, topPos + 9, 0xFFFFFFFF);

        String leftTitle = "§f" + Component.translatable(boardType == 0 ? "r3ct_daily.leaderboard.left.quests" : "r3ct_daily.leaderboard.left.rewards").getString();
        String rightTitle = "§f" + Component.translatable("r3ct_daily.leaderboard.right.streak").getString();

        guiGraphics.centeredText(this.font, leftTitle, leftPos + (boardWidth / 4) + 1, topPos + 33, 0xFFFFFFFF);
        guiGraphics.centeredText(this.font, rightTitle, leftPos + (3 * boardWidth / 4) - 1, topPos + 33, 0xFFFFFFFF);

        hoveredEntry = null;
        renderColumn(guiGraphics, leftList, leftPos + 14, topPos + 52, mouseX, mouseY, true, leftScrollOffset);
        renderColumn(guiGraphics, rightList, midX + 12, topPos + 52, mouseX, mouseY, false, rightScrollOffset);

        String switchText = Component.translatable(boardType == 0 ? "r3ct_daily.leaderboard.button.rewards_next" : "r3ct_daily.leaderboard.button.quests_prev").getString();
        int switchWidth = this.font.width(switchText);
        int switchX = (boardType == 0) ? leftPos + boardWidth + 15 : leftPos - 15 - switchWidth;
        int switchY = (this.height / 2) - 4;

        boolean switchHover = mouseX >= switchX && mouseX <= switchX + switchWidth && mouseY >= switchY - 2 && mouseY <= switchY + 10;
        int switchColor = switchHover ? 0xFFFFFFFF : 0xFFAAAAAA;
        guiGraphics.text(this.font, switchText, switchX, switchY, switchColor, true);

        String backText = Component.translatable("r3ct_daily.leaderboard.button.back").getString();
        int backWidth = this.font.width(backText);
        int backX = midX - (backWidth / 2);
        int backY = topPos + boardHeight + 8;

        boolean backHover = mouseX >= backX - 2 && mouseX <= backX + backWidth + 2 && mouseY >= backY - 2 && mouseY <= backY + 10;
        int backColor = backHover ? 0xFFFF5555 : 0xFFAAAAAA;
        guiGraphics.text(this.font, backText, backX, backY, backColor, true);

        if (hoveredEntry != null) {
            List<ClientTooltipComponent> tooltipLines = new ArrayList<>();

            String questsLabel = Component.translatable("r3ct_daily.leaderboard.tooltip.quests_completed").getString();
            String maxQuestLabel = Component.translatable("r3ct_daily.leaderboard.tooltip.max_quest_streak").getString();
            String rewardsLabel = Component.translatable("r3ct_daily.leaderboard.tooltip.rewards_collected").getString();
            String maxRewardLabel = Component.translatable("r3ct_daily.leaderboard.tooltip.max_reward_streak").getString();
            String daysLabel = Component.translatable("r3ct_daily.leaderboard.tooltip.days").getString();

            tooltipLines.add(ClientTooltipComponent.create(Component.literal("     §f§l" + hoveredEntry.name()).getVisualOrderText()));
            tooltipLines.add(ClientTooltipComponent.create(Component.literal("§8----------------").getVisualOrderText()));
            tooltipLines.add(ClientTooltipComponent.create(Component.literal("§f" + questsLabel + ": §e" + hoveredEntry.totalQuests()).getVisualOrderText()));
            tooltipLines.add(ClientTooltipComponent.create(Component.literal("§f" + maxQuestLabel + ": §e" + hoveredEntry.maxQuestStreak() + " " + daysLabel).getVisualOrderText()));
            tooltipLines.add(ClientTooltipComponent.create(Component.literal("§f" + rewardsLabel + ": §e" + hoveredEntry.totalRewards()).getVisualOrderText()));
            tooltipLines.add(ClientTooltipComponent.create(Component.literal("§f" + maxRewardLabel + ": §e" + hoveredEntry.maxRewardStreak() + " " + daysLabel).getVisualOrderText()));

            guiGraphics.tooltip(this.font, tooltipLines, mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null, true);

            ItemStack head = new ItemStack(Items.PLAYER_HEAD);
            head.set(DataComponents.PROFILE, ResolvableProfile.createUnresolved(hoveredEntry.name()));
            guiGraphics.item(head, mouseX + 11, mouseY - 14);
        }

        guiGraphics.pose().popMatrix();
    }

    private void drawHallOfFameBoard(GuiGraphicsExtractor g, int leftPos, int topPos, int midX) {
        int rightPos = leftPos + boardWidth;
        int bottomPos = topPos + boardHeight;

        g.fill(leftPos - 3, topPos - 3, rightPos + 3, bottomPos + 3, 0xFF1A0E07);
        g.fill(leftPos - 2, topPos - 2, rightPos + 2, bottomPos + 2, 0xFF4A2B18);
        g.fill(leftPos - 2, topPos - 2, rightPos + 2, topPos - 1, 0xFF6B4226);
        g.fill(leftPos + 2, topPos + 2, rightPos - 2, bottomPos - 2, 0xFFB8860B);
        g.fill(leftPos + 3, topPos + 3, rightPos - 3, bottomPos - 3, 0xFF2B180D);

        g.fill(leftPos + 6, topPos + 5, rightPos - 6, topPos + 22, 0xFF8C6D1F);
        g.fill(leftPos + 7, topPos + 6, rightPos - 7, topPos + 21, 0xFF33131A);
        g.fill(leftPos + 7, topPos + 6, rightPos - 7, topPos + 8, 0x55000000);

        g.fill(leftPos + 12, topPos + 12, leftPos + 15, topPos + 15, 0xFFFFE066);
        g.fill(rightPos - 15, topPos + 12, rightPos - 12, topPos + 15, 0xFFFFE066);

        g.fill(leftPos + 2, topPos + 23, rightPos - 2, topPos + 26, 0xFF1E1008);
        g.fill(leftPos + 3, topPos + 24, rightPos - 3, topPos + 25, 0xFF5A351E);

        drawColumnBay(g, leftPos + 8, topPos + 28, midX - 5, bottomPos - 7);
        drawColumnBay(g, midX + 5, topPos + 28, rightPos - 8, bottomPos - 7);

        g.fill(midX - 4, topPos + 25, midX + 4, bottomPos - 3, 0xFF1E1008);
        g.fill(midX - 3, topPos + 26, midX + 3, bottomPos - 4, 0xFF4A2B18);
        g.fill(midX - 1, topPos + 28, midX + 1, bottomPos - 6, 0xFF9E7B22);

        int medalY = topPos + 142;
        g.fill(midX - 5, medalY - 5, midX + 5, medalY + 5, 0xFF241209);
        g.fill(midX - 4, medalY - 4, midX + 4, medalY + 4, 0xFFD4AF37);
        g.fill(midX - 2, medalY - 2, midX + 2, medalY + 2, 0xFFFFF2B2);

        drawCornerBracket(g, leftPos - 3, topPos - 3, true, true);
        drawCornerBracket(g, rightPos - 9, topPos - 3, false, true);
        drawCornerBracket(g, leftPos - 3, bottomPos - 9, true, false);
        drawCornerBracket(g, rightPos - 9, bottomPos - 9, false, false);
    }

    private void drawColumnBay(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2) {
        g.fill(x1 - 1, y1 - 1, x2 + 1, y2 + 1, 0xFF6E551C);
        g.fill(x1, y1, x2, y2, 0xFF1D1418);

        g.fill(x1 + 2, y1 + 2, x2 - 2, y1 + 18, 0xFF8C6D1F);
        g.fill(x1 + 3, y1 + 3, x2 - 3, y1 + 17, 0xFF3A2218);
        g.fill(x1 + 4, y1 + 4, x2 - 4, y1 + 5, 0xFF593728);
        g.fill(x1 + 6, y1 + 9, x1 + 8, y1 + 11, 0xFFD4AF37);
        g.fill(x2 - 8, y1 + 9, x2 - 6, y1 + 11, 0xFFD4AF37);

        g.fill(x1, y1 + 19, x2, y1 + 22, 0x66000000);
        g.fill(x1, y1 + 19, x1 + 2, y2, 0x44000000);
        g.fill(x2 - 2, y1 + 19, x2, y2, 0x44000000);

        int sepY = y1 + 207;
        g.fill(x1 + 2, sepY - 1, x2 - 2, sepY + 3, 0x55000000);
        g.fill(x1 + 3, sepY, x2 - 3, sepY + 1, 0xFF8C6D1F);
        g.fill(x1 + 3, sepY + 1, x2 - 3, sepY + 2, 0xFF3A2218);
    }

    private void drawCornerBracket(GuiGraphicsExtractor g, int x, int y, boolean left, boolean top) {
        int goldDark = 0xFF8C6D1F;
        int goldBright = 0xFFE6C259;
        int rivet = 0xFFFFF2B2;

        int hY = top ? y : y + 8;
        g.fill(x, hY, x + 12, hY + 4, goldDark);
        g.fill(x + 1, hY + 1, x + 11, hY + 2, goldBright);

        int vX = left ? x : x + 8;
        g.fill(vX, y, vX + 4, y + 12, goldDark);
        g.fill(vX + 1, y + 1, vX + 2, y + 11, goldBright);

        g.fill(vX + 1, hY + 1, vX + 3, hY + 3, rivet);
    }

    private void renderColumn(GuiGraphicsExtractor guiGraphics, List<TopEntry> rawList, int startX, int startY, int mouseX, int mouseY, boolean isLeftColumn, int scrollOffset) {
        String myName = getClientPlayerName();
        TopEntry myEntry = null;
        int myRank = -1;

        int top100Count = getTop100Count(rawList, isLeftColumn);
        int[] ranks = new int[top100Count];

        int currentRank = 1;
        int previousScore = -1;

        for (int i = 0; i < rawList.size(); i++) {
            TopEntry entry = rawList.get(i);
            int score = getScore(entry, isLeftColumn);

            if (i < top100Count) {
                if (i > 0 && score < previousScore) {
                    currentRank++;
                }
                previousScore = score;
                ranks[i] = currentRank;

                if (!myName.isEmpty() && entry.name().equalsIgnoreCase(myName)) {
                    myEntry = entry;
                    myRank = currentRank;
                }
            } else {
                if (!myName.isEmpty() && entry.name().equalsIgnoreCase(myName)) {
                    myEntry = entry;
                }
            }
        }

        if (myEntry == null) {
            for (TopEntry e : this.leftList) {
                if (e.name().equalsIgnoreCase(myName)) {
                    myEntry = e;
                    break;
                }
            }
        }
        if (myEntry == null) {
            for (TopEntry e : this.rightList) {
                if (e.name().equalsIgnoreCase(myName)) {
                    myEntry = e;
                    break;
                }
            }
        }
        if (myEntry == null) {
            myEntry = new TopEntry(myName.isEmpty() ? "-" : myName, 0, 0, 0, 0);
        }

        int maxScroll = Math.max(0, top100Count - VISIBLE_ROWS);
        int clampedScroll = Math.max(0, Math.min(scrollOffset, maxScroll));

        for (int visIdx = 0; visIdx < VISIBLE_ROWS; visIdx++) {
            int dataIdx = clampedScroll + visIdx;
            if (dataIdx >= top100Count) break;

            TopEntry entry = rawList.get(dataIdx);
            int rank = ranks[dataIdx];
            int score = getScore(entry, isLeftColumn);
            int rowY = startY + (visIdx * ROW_STEP);

            drawLeaderboardRow(guiGraphics, entry, String.valueOf(rank) + ".", rank, score, startX, rowY, mouseX, mouseY, dataIdx, false);
        }

        int scrollbarX = startX + ROW_WIDTH + 3;
        int trackTop = startY;
        int trackHeight = (VISIBLE_ROWS * ROW_STEP) - 1;
        drawScrollbar(guiGraphics, scrollbarX, trackTop, trackHeight, clampedScroll, maxScroll, top100Count, mouseX, mouseY);

        int pinnedY = startY + (VISIBLE_ROWS * ROW_STEP) + 10;
        String myRankLabel = (myRank > 0) ? (myRank + ".") : "-";
        int myScore = getScore(myEntry, isLeftColumn);
        drawLeaderboardRow(guiGraphics, myEntry, myRankLabel, -1, myScore, startX, pinnedY, mouseX, mouseY, 0, true);
    }

    private void drawLeaderboardRow(GuiGraphicsExtractor guiGraphics, TopEntry entry, String rankLabel, int rank, int scoreToDisplay, int startX, int y, int mouseX, int mouseY, int stripeIdx, boolean isPinnedSelf) {
        boolean isRowHovered = mouseX >= startX - 2 && mouseX <= startX + ROW_WIDTH && mouseY >= y && mouseY <= y + 16;
        if (isRowHovered) {
            hoveredEntry = entry;
        }

        int rowBorder;
        int rowBg;
        String nameColor;
        String valColor;

        if (isPinnedSelf) {
            rowBorder = isRowHovered ? 0xFF88FFFF : 0xFF2E7D8C;
            rowBg = 0xFF17282E;
            nameColor = "§b§l";
            valColor = "§b";
        } else if (rank == 1) {
            rowBorder = isRowHovered ? 0xFFFF6666 : 0xFFB71C1C;
            rowBg = 0xFF331416;
            nameColor = "§c§l";
            valColor = "§c";
        } else if (rank == 2) {
            rowBorder = isRowHovered ? 0xFFFF88FF : 0xFF8E3A9E;
            rowBg = 0xFF2D1934;
            nameColor = "§d§l";
            valColor = "§d";
        } else if (rank == 3) {
            rowBorder = isRowHovered ? 0xFFFFE57F : 0xFFA67C1E;
            rowBg = 0xFF2E2416;
            nameColor = "§e§l";
            valColor = "§e";
        } else {
            rowBorder = isRowHovered ? 0xFF8D6E63 : 0xFF332428;
            rowBg = (stripeIdx % 2 == 0) ? 0xFF251A1E : 0xFF20161A;
            nameColor = "§7";
            valColor = "§f";
        }

        guiGraphics.fill(startX - 2, y, startX + ROW_WIDTH, y + 17, rowBorder);
        guiGraphics.fill(startX - 1, y + 1, startX + ROW_WIDTH - 1, y + 16, rowBg);

        guiGraphics.fill(startX - 1, y + 1, startX + 17, y + 16, 0x44000000);
        guiGraphics.fill(startX + 17, y + 1, startX + 18, y + 16, rowBorder);

        if (isRowHovered) {
            guiGraphics.fill(startX - 1, y + 1, startX + ROW_WIDTH - 1, y + 16, 0x22FFFFFF);
        }

        ItemStack head = new ItemStack(Items.PLAYER_HEAD);
        head.set(DataComponents.PROFILE, ResolvableProfile.createUnresolved(entry.name()));
        guiGraphics.item(head, startX, y);

        String rankAndName = "§6" + rankLabel + " " + nameColor + entry.name();
        String scoreTxt = valColor + scoreToDisplay;
        int scoreWidth = this.font.width(scoreTxt);

        int maxNameWidth = (ROW_WIDTH - 28) - scoreWidth;
        if (this.font.width(rankAndName) > maxNameWidth && maxNameWidth > 15) {
            rankAndName = this.font.plainSubstrByWidth(rankAndName, maxNameWidth - this.font.width("...")) + "...";
        }

        guiGraphics.text(this.font, rankAndName, startX + 21, y + 5, 0xFFFFFFFF, true);
        guiGraphics.text(this.font, scoreTxt, startX + ROW_WIDTH - 5 - scoreWidth, y + 5, 0xFFFFFFFF, true);
    }

    private void drawScrollbar(GuiGraphicsExtractor g, int x, int y, int height, int scrollOffset, int maxScroll, int totalCount, int mouseX, int mouseY) {
        g.fill(x, y, x + 6, y + height, 0xFF120B0E);
        g.fill(x + 1, y + 1, x + 5, y + height - 1, 0xFF261A1E);

        if (maxScroll <= 0 || totalCount <= VISIBLE_ROWS) {
            g.fill(x + 1, y + 1, x + 5, y + height - 1, 0xFF4A3525);
            return;
        }

        int thumbHeight = Math.max(18, (int)((float) VISIBLE_ROWS / totalCount * (height - 2)));
        int scrollableTrack = (height - 2) - thumbHeight;
        int thumbY = y + 1 + (int)((float) scrollOffset / maxScroll * scrollableTrack);

        boolean hovered = mouseX >= x && mouseX <= x + 6 && mouseY >= thumbY && mouseY <= thumbY + thumbHeight;
        int thumbBorder = hovered ? 0xFFFFE066 : 0xFFB8860B;
        int thumbFill = hovered ? 0xFFD4AF37 : 0xFF8C6D1F;

        g.fill(x + 1, thumbY, x + 5, thumbY + thumbHeight, thumbBorder);
        g.fill(x + 2, thumbY + 1, x + 4, thumbY + thumbHeight - 1, thumbFill);
    }

    private void updateScrollbarDrag(double rawMouseY, boolean isLeft) {
        float scale = DailyClientConfig.getInstance().leaderboardScreenScale;
        int mY = (int)((rawMouseY - this.height / 2f) / scale + this.height / 2f);
        int topPos = (this.height - boardHeight) / 2;
        int trackTop = topPos + 52;
        int trackHeight = (VISIBLE_ROWS * ROW_STEP) - 1;

        int top100Count = getTop100Count(isLeft ? leftList : rightList, isLeft);
        int maxScroll = Math.max(0, top100Count - VISIBLE_ROWS);
        if (maxScroll <= 0) return;

        float ratio = (float)(mY - trackTop) / (float) trackHeight;
        ratio = Math.max(0.0f, Math.min(1.0f, ratio));
        int newScroll = Math.round(ratio * maxScroll);

        if (isLeft) {
            leftScrollOffset = newScroll;
        } else {
            rightScrollOffset = newScroll;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY == 0) return false;

        float scale = DailyClientConfig.getInstance().leaderboardScreenScale;
        int mX = (int)((mouseX - this.width / 2f) / scale + this.width / 2f);
        int mY = (int)((mouseY - this.height / 2f) / scale + this.height / 2f);

        int leftPos = (this.width - boardWidth) / 2;
        int topPos = (this.height - boardHeight) / 2;
        int midX = leftPos + (boardWidth / 2);

        if (mY >= topPos + 48 && mY <= topPos + 52 + (VISIBLE_ROWS * ROW_STEP)) {
            int delta = (scrollY > 0) ? -1 : 1;
            if (mX >= leftPos && mX < midX) {
                int maxLeft = Math.max(0, getTop100Count(leftList, true) - VISIBLE_ROWS);
                leftScrollOffset = Math.max(0, Math.min(maxLeft, leftScrollOffset + delta));
                return true;
            } else if (mX >= midX && mX <= leftPos + boardWidth) {
                int maxRight = Math.max(0, getTop100Count(rightList, false) - VISIBLE_ROWS);
                rightScrollOffset = Math.max(0, Math.min(maxRight, rightScrollOffset + delta));
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
        if (event.button() == SDLMouse.SDL_BUTTON_LEFT) {
            float scale = DailyClientConfig.getInstance().leaderboardScreenScale;

            int mX = (int)((event.x() - this.width / 2f) / scale + this.width / 2f);
            int mY = (int)((event.y() - this.height / 2f) / scale + this.height / 2f);

            int leftPos = (this.width - boardWidth) / 2;
            int topPos = (this.height - boardHeight) / 2;
            int midX = leftPos + (boardWidth / 2);

            int trackTop = topPos + 52;
            int trackBottom = trackTop + (VISIBLE_ROWS * ROW_STEP) - 1;
            int leftScrollbarX = leftPos + 14 + ROW_WIDTH + 3;
            int rightScrollbarX = midX + 12 + ROW_WIDTH + 3;

            if (mY >= trackTop && mY <= trackBottom) {
                if (mX >= leftScrollbarX - 2 && mX <= leftScrollbarX + 8) {
                    draggingScrollbar = 1;
                    updateScrollbarDrag(event.y(), true);
                    return true;
                }
                if (mX >= rightScrollbarX - 2 && mX <= rightScrollbarX + 8) {
                    draggingScrollbar = 2;
                    updateScrollbarDrag(event.y(), false);
                    return true;
                }
            }

            String switchText = Component.translatable(boardType == 0 ? "r3ct_daily.leaderboard.button.rewards_next" : "r3ct_daily.leaderboard.button.quests_prev").getString();
            int switchWidth = this.font.width(switchText);
            int switchX = (boardType == 0) ? leftPos + boardWidth + 15 : leftPos - 15 - switchWidth;
            int switchY = (this.height / 2) - 4;

            if (mX >= switchX && mX <= switchX + switchWidth && mY >= switchY - 2 && mY <= switchY + 10) {
                if (this.minecraft != null && this.minecraft.player != null) {
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
                    Services.PLATFORM.sendToServer(new RequestLeaderboardPayload(boardType == 0 ? 1 : 0));
                    return true;
                }
            }

            String backText = Component.translatable("r3ct_daily.leaderboard.button.back").getString();
            int backWidth = this.font.width(backText);
            int backX = midX - (backWidth / 2);
            int backY = topPos + boardHeight + 8;

            if (mX >= backX - 2 && mX <= backX + backWidth + 2 && mY >= backY - 2 && mY <= backY + 10) {
                if (this.minecraft != null && this.minecraft.player != null) {
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    this.minecraft.player.connection.sendCommand(boardType == 0 ? "daily quests" : "daily rewards");
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (event.button() == SDLMouse.SDL_BUTTON_LEFT && draggingScrollbar != 0) {
            updateScrollbarDrag(event.y(), draggingScrollbar == 1);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == SDLMouse.SDL_BUTTON_LEFT && draggingScrollbar != 0) {
            draggingScrollbar = 0;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_ESCAPE) {
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.connection.sendCommand(this.boardType == 0 ? "daily quests" : "daily rewards");
            }
            return true;
        }

        if (Services.PLATFORM.isQuestKey(event)) {
            this.onClose();
            return true;
        }
        if (Services.PLATFORM.isRewardKey(event)) {
            this.onClose();
            return true;
        }

        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}