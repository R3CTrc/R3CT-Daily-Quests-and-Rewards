package com.r3ct.daily.logic;

import com.r3ct.daily.data.ModState;
import com.r3ct.daily.data.PlayerData;
import com.r3ct.daily.data.TopEntry;
import com.r3ct.daily.network.LeaderboardResponsePayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.ToIntFunction;

public class LeaderboardManager {
    public static final int MAX_TOP_ENTRIES = 100;

    public static LeaderboardResponsePayload cachedQuestsBoard = null;
    public static LeaderboardResponsePayload cachedRewardsBoard = null;
    public static int lastLeaderboardUpdateTick = -1;

    private static int compareNames(PlayerData a, PlayerData b) {
        String nameA = a.lastKnownName != null ? a.lastKnownName : "";
        String nameB = b.lastKnownName != null ? b.lastKnownName : "";
        return nameA.compareToIgnoreCase(nameB);
    }

    private static int calculateDenseRank(ModState state, int playerScore, ToIntFunction<PlayerData> scoreExtractor) {
        if (playerScore <= 0) return 0;
        Set<Integer> higherScores = new HashSet<>();
        for (PlayerData pd : state.players.values()) {
            int score = scoreExtractor.applyAsInt(pd);
            if (score > playerScore) {
                higherScores.add(score);
            }
        }
        return 1 + higherScores.size();
    }

    public static void updateLeaderboardCache(MinecraftServer server) {
        ModState state = ModState.get(server);
        List<Map.Entry<UUID, PlayerData>> allPlayers = new ArrayList<>(state.players.entrySet());

        List<TopEntry> leftQ = new ArrayList<>();
        List<TopEntry> rightQ = new ArrayList<>();

        allPlayers.sort((a, b) -> {
            int cmp = Integer.compare(b.getValue().totalQuestsCompleted, a.getValue().totalQuestsCompleted);
            return cmp != 0 ? cmp : compareNames(a.getValue(), b.getValue());
        });
        int currentRank = 1;
        int prevScore = -1;
        for (int i = 0; i < allPlayers.size() && leftQ.size() < MAX_TOP_ENTRIES; i++) {
            PlayerData p = allPlayers.get(i).getValue();
            int score = p.totalQuestsCompleted;
            if (score > 0) {
                if (!leftQ.isEmpty() && score < prevScore) currentRank++;
                prevScore = score;
                leftQ.add(new TopEntry(p.lastKnownName, p.totalQuestsCompleted, p.maxQuestStreak, p.totalCollected, p.maxRewardStreak, currentRank));
            }
        }

        allPlayers.sort((a, b) -> {
            int cmp = Integer.compare(b.getValue().maxQuestStreak, a.getValue().maxQuestStreak);
            return cmp != 0 ? cmp : compareNames(a.getValue(), b.getValue());
        });
        currentRank = 1;
        prevScore = -1;
        for (int i = 0; i < allPlayers.size() && rightQ.size() < MAX_TOP_ENTRIES; i++) {
            PlayerData p = allPlayers.get(i).getValue();
            int score = p.maxQuestStreak;
            if (score > 0) {
                if (!rightQ.isEmpty() && score < prevScore) currentRank++;
                prevScore = score;
                rightQ.add(new TopEntry(p.lastKnownName, p.totalQuestsCompleted, p.maxQuestStreak, p.totalCollected, p.maxRewardStreak, currentRank));
            }
        }
        cachedQuestsBoard = new LeaderboardResponsePayload(0, leftQ, rightQ);

        List<TopEntry> leftR = new ArrayList<>();
        List<TopEntry> rightR = new ArrayList<>();

        allPlayers.sort((a, b) -> {
            int cmp = Integer.compare(b.getValue().totalCollected, a.getValue().totalCollected);
            return cmp != 0 ? cmp : compareNames(a.getValue(), b.getValue());
        });
        currentRank = 1;
        prevScore = -1;
        for (int i = 0; i < allPlayers.size() && leftR.size() < MAX_TOP_ENTRIES; i++) {
            PlayerData p = allPlayers.get(i).getValue();
            int score = p.totalCollected;
            if (score > 0) {
                if (!leftR.isEmpty() && score < prevScore) currentRank++;
                prevScore = score;
                leftR.add(new TopEntry(p.lastKnownName, p.totalQuestsCompleted, p.maxQuestStreak, p.totalCollected, p.maxRewardStreak, currentRank));
            }
        }

        allPlayers.sort((a, b) -> {
            int cmp = Integer.compare(b.getValue().maxRewardStreak, a.getValue().maxRewardStreak);
            return cmp != 0 ? cmp : compareNames(a.getValue(), b.getValue());
        });
        currentRank = 1;
        prevScore = -1;
        for (int i = 0; i < allPlayers.size() && rightR.size() < MAX_TOP_ENTRIES; i++) {
            PlayerData p = allPlayers.get(i).getValue();
            int score = p.maxRewardStreak;
            if (score > 0) {
                if (!rightR.isEmpty() && score < prevScore) currentRank++;
                prevScore = score;
                rightR.add(new TopEntry(p.lastKnownName, p.totalQuestsCompleted, p.maxQuestStreak, p.totalCollected, p.maxRewardStreak, currentRank));
            }
        }
        cachedRewardsBoard = new LeaderboardResponsePayload(1, leftR, rightR);
    }

    public static LeaderboardResponsePayload getBoardForPlayer(MinecraftServer server, ServerPlayer player, int boardType) {
        LeaderboardResponsePayload base = (boardType == 0) ? cachedQuestsBoard : cachedRewardsBoard;
        if (base == null || player == null) return base;

        ModState state = ModState.get(server);
        PlayerData pData = ModState.getPlayerData(server, player.getUUID());
        String playerName = player.getGameProfile().getName();

        List<TopEntry> leftCopy = new ArrayList<>(base.leftList());
        if (leftCopy.stream().noneMatch(e -> e.name().equalsIgnoreCase(playerName))) {
            int leftRank = (boardType == 0)
                    ? calculateDenseRank(state, pData.totalQuestsCompleted, pd -> pd.totalQuestsCompleted)
                    : calculateDenseRank(state, pData.totalCollected, pd -> pd.totalCollected);
            leftCopy.add(new TopEntry(playerName, pData.totalQuestsCompleted, pData.maxQuestStreak, pData.totalCollected, pData.maxRewardStreak, leftRank));
        }

        List<TopEntry> rightCopy = new ArrayList<>(base.rightList());
        if (rightCopy.stream().noneMatch(e -> e.name().equalsIgnoreCase(playerName))) {
            int rightRank = (boardType == 0)
                    ? calculateDenseRank(state, pData.maxQuestStreak, pd -> pd.maxQuestStreak)
                    : calculateDenseRank(state, pData.maxRewardStreak, pd -> pd.maxRewardStreak);
            rightCopy.add(new TopEntry(playerName, pData.totalQuestsCompleted, pData.maxQuestStreak, pData.totalCollected, pData.maxRewardStreak, rightRank));
        }

        return new LeaderboardResponsePayload(boardType, leftCopy, rightCopy);
    }
}