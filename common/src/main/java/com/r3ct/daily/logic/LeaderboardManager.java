package com.r3ct.daily.logic;

import com.r3ct.daily.data.ModState;
import com.r3ct.daily.data.PlayerData;
import com.r3ct.daily.data.TopEntry;
import com.r3ct.daily.network.LeaderboardResponsePayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    public static void updateLeaderboardCache(MinecraftServer server) {
        ModState state = ModState.get(server);
        List<Map.Entry<UUID, PlayerData>> allPlayers = new ArrayList<>(state.players.entrySet());

        List<TopEntry> questCompletionList = new ArrayList<>();
        List<TopEntry> questStreakList = new ArrayList<>();

        allPlayers.sort((playerA, playerB) -> {
            int cmp = Integer.compare(playerB.getValue().totalQuestsCompleted, playerA.getValue().totalQuestsCompleted);
            return cmp != 0 ? cmp : compareNames(playerA.getValue(), playerB.getValue());
        });
        for (int i = 0; i < allPlayers.size() && questCompletionList.size() < MAX_TOP_ENTRIES; i++) {
            PlayerData player = allPlayers.get(i).getValue();
            if (player.totalQuestsCompleted > 0) {
                questCompletionList.add(new TopEntry(player.lastKnownName, player.totalQuestsCompleted, player.maxQuestStreak, player.totalCollected, player.maxRewardStreak));
            }
        }

        allPlayers.sort((playerA, playerB) -> {
            int cmp = Integer.compare(playerB.getValue().maxQuestStreak, playerA.getValue().maxQuestStreak);
            return cmp != 0 ? cmp : compareNames(playerA.getValue(), playerB.getValue());
        });
        for (int i = 0; i < allPlayers.size() && questStreakList.size() < MAX_TOP_ENTRIES; i++) {
            PlayerData player = allPlayers.get(i).getValue();
            if (player.maxQuestStreak > 0) {
                questStreakList.add(new TopEntry(player.lastKnownName, player.totalQuestsCompleted, player.maxQuestStreak, player.totalCollected, player.maxRewardStreak));
            }
        }
        cachedQuestsBoard = new LeaderboardResponsePayload(0, questCompletionList, questStreakList);

        List<TopEntry> rewardCollectionList = new ArrayList<>();
        List<TopEntry> rewardStreakList = new ArrayList<>();

        allPlayers.sort((playerA, playerB) -> {
            int cmp = Integer.compare(playerB.getValue().totalCollected, playerA.getValue().totalCollected);
            return cmp != 0 ? cmp : compareNames(playerA.getValue(), playerB.getValue());
        });
        for (int i = 0; i < allPlayers.size() && rewardCollectionList.size() < MAX_TOP_ENTRIES; i++) {
            PlayerData player = allPlayers.get(i).getValue();
            if (player.totalCollected > 0) {
                rewardCollectionList.add(new TopEntry(player.lastKnownName, player.totalQuestsCompleted, player.maxQuestStreak, player.totalCollected, player.maxRewardStreak));
            }
        }

        allPlayers.sort((playerA, playerB) -> {
            int cmp = Integer.compare(playerB.getValue().maxRewardStreak, playerA.getValue().maxRewardStreak);
            return cmp != 0 ? cmp : compareNames(playerA.getValue(), playerB.getValue());
        });
        for (int i = 0; i < allPlayers.size() && rewardStreakList.size() < MAX_TOP_ENTRIES; i++) {
            PlayerData player = allPlayers.get(i).getValue();
            if (player.maxRewardStreak > 0) {
                rewardStreakList.add(new TopEntry(player.lastKnownName, player.totalQuestsCompleted, player.maxQuestStreak, player.totalCollected, player.maxRewardStreak));
            }
        }
        cachedRewardsBoard = new LeaderboardResponsePayload(1, rewardCollectionList, rewardStreakList);
    }

    public static LeaderboardResponsePayload getBoardForPlayer(MinecraftServer server, ServerPlayer player, int boardType) {
        LeaderboardResponsePayload base = (boardType == 0) ? cachedQuestsBoard : cachedRewardsBoard;
        if (base == null || player == null) return base;

        PlayerData pData = ModState.getPlayerData(server, player.getUUID());
        String playerName = player.getGameProfile().name();
        TopEntry selfEntry = new TopEntry(playerName, pData.totalQuestsCompleted, pData.maxQuestStreak, pData.totalCollected, pData.maxRewardStreak);

        List<TopEntry> leftCopy = new ArrayList<>(base.leftList());
        if (leftCopy.stream().noneMatch(e -> e.name().equalsIgnoreCase(playerName))) {
            leftCopy.add(selfEntry);
        }

        List<TopEntry> rightCopy = new ArrayList<>(base.rightList());
        if (rightCopy.stream().noneMatch(e -> e.name().equalsIgnoreCase(playerName))) {
            rightCopy.add(selfEntry);
        }

        return new LeaderboardResponsePayload(boardType, leftCopy, rightCopy);
    }
}