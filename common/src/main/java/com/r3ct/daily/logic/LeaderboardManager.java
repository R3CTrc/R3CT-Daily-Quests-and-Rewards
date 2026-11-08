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

        List<TopEntry> questCompletionList = new ArrayList<>();
        List<TopEntry> questStreakList = new ArrayList<>();

        allPlayers.sort((playerA, playerB) -> {
            int cmp = Integer.compare(playerB.getValue().totalQuestsCompleted, playerA.getValue().totalQuestsCompleted);
            return cmp != 0 ? cmp : compareNames(playerA.getValue(), playerB.getValue());
        });
        int currentRank = 1;
        int prevScore = -1;
        for (int i = 0; i < allPlayers.size() && questCompletionList.size() < MAX_TOP_ENTRIES; i++) {
            PlayerData player = allPlayers.get(i).getValue();
            int score = player.totalQuestsCompleted;
            if (score > 0) {
                if (!questCompletionList.isEmpty() && score < prevScore) currentRank++;
                prevScore = score;
                questCompletionList.add(new TopEntry(player.lastKnownName, player.totalQuestsCompleted, player.maxQuestStreak, player.totalCollected, player.maxRewardStreak, currentRank));
            }
        }

        allPlayers.sort((playerA, playerB) -> {
            int cmp = Integer.compare(playerB.getValue().maxQuestStreak, playerA.getValue().maxQuestStreak);
            return cmp != 0 ? cmp : compareNames(playerA.getValue(), playerB.getValue());
        });
        currentRank = 1;
        prevScore = -1;
        for (int i = 0; i < allPlayers.size() && questStreakList.size() < MAX_TOP_ENTRIES; i++) {
            PlayerData player = allPlayers.get(i).getValue();
            int score = player.maxQuestStreak;
            if (score > 0) {
                if (!questStreakList.isEmpty() && score < prevScore) currentRank++;
                prevScore = score;
                questStreakList.add(new TopEntry(player.lastKnownName, player.totalQuestsCompleted, player.maxQuestStreak, player.totalCollected, player.maxRewardStreak, currentRank));
            }
        }
        cachedQuestsBoard = new LeaderboardResponsePayload(0, questCompletionList, questStreakList);

        List<TopEntry> rewardCollectionList = new ArrayList<>();
        List<TopEntry> rewardStreakList = new ArrayList<>();

        allPlayers.sort((playerA, playerB) -> {
            int cmp = Integer.compare(playerB.getValue().totalCollected, playerA.getValue().totalCollected);
            return cmp != 0 ? cmp : compareNames(playerA.getValue(), playerB.getValue());
        });
        currentRank = 1;
        prevScore = -1;
        for (int i = 0; i < allPlayers.size() && rewardCollectionList.size() < MAX_TOP_ENTRIES; i++) {
            PlayerData player = allPlayers.get(i).getValue();
            int score = player.totalCollected;
            if (score > 0) {
                if (!rewardCollectionList.isEmpty() && score < prevScore) currentRank++;
                prevScore = score;
                rewardCollectionList.add(new TopEntry(player.lastKnownName, player.totalQuestsCompleted, player.maxQuestStreak, player.totalCollected, player.maxRewardStreak, currentRank));
            }
        }

        allPlayers.sort((playerA, playerB) -> {
            int cmp = Integer.compare(playerB.getValue().maxRewardStreak, playerA.getValue().maxRewardStreak);
            return cmp != 0 ? cmp : compareNames(playerA.getValue(), playerB.getValue());
        });
        currentRank = 1;
        prevScore = -1;
        for (int i = 0; i < allPlayers.size() && rewardStreakList.size() < MAX_TOP_ENTRIES; i++) {
            PlayerData player = allPlayers.get(i).getValue();
            int score = player.maxRewardStreak;
            if (score > 0) {
                if (!rewardStreakList.isEmpty() && score < prevScore) currentRank++;
                prevScore = score;
                rewardStreakList.add(new TopEntry(player.lastKnownName, player.totalQuestsCompleted, player.maxQuestStreak, player.totalCollected, player.maxRewardStreak, currentRank));
            }
        }
        cachedRewardsBoard = new LeaderboardResponsePayload(1, rewardCollectionList, rewardStreakList);
    }

    public static LeaderboardResponsePayload getBoardForPlayer(MinecraftServer server, ServerPlayer player, int boardType) {
        LeaderboardResponsePayload base = (boardType == 0) ? cachedQuestsBoard : cachedRewardsBoard;
        if (base == null || player == null) return base;

        ModState state = ModState.get(server);
        PlayerData pData = ModState.getPlayerData(server, player.getUUID());
        String playerName = player.getGameProfile().name();

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