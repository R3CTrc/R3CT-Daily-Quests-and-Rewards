package com.r3ct.daily.logic;

import com.r3ct.daily.data.ModState;
import com.r3ct.daily.data.PlayerData;
import com.r3ct.daily.data.TopEntry;
import com.r3ct.daily.network.LeaderboardResponsePayload;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LeaderboardManager {
    public static LeaderboardResponsePayload cachedQuestsBoard = null;
    public static LeaderboardResponsePayload cachedRewardsBoard = null;
    public static int lastLeaderboardUpdateTick = -1;

    public static void updateLeaderboardCache(MinecraftServer server) {
        ModState state = ModState.get(server);
        List<Map.Entry<UUID, PlayerData>> allPlayers = new ArrayList<>(state.players.entrySet());

        List<TopEntry> questCompletionList = new ArrayList<>();
        List<TopEntry> questStreakList = new ArrayList<>();

        allPlayers.sort((playerA, playerB) -> Integer.compare(playerB.getValue().totalQuestsCompleted, playerA.getValue().totalQuestsCompleted));
        for (int i = 0; i < allPlayers.size() && questCompletionList.size() < 10; i++) {
            PlayerData player = allPlayers.get(i).getValue();
            if (player.totalQuestsCompleted > 0) questCompletionList.add(new TopEntry(player.lastKnownName, player.totalQuestsCompleted, player.maxQuestStreak, player.totalCollected, player.maxRewardStreak));
        }

        allPlayers.sort((playerA, playerB) -> Integer.compare(playerB.getValue().maxQuestStreak, playerA.getValue().maxQuestStreak));
        for (int i = 0; i < allPlayers.size() && questStreakList.size() < 10; i++) {
            PlayerData player = allPlayers.get(i).getValue();
            if (player.maxQuestStreak > 0) questStreakList.add(new TopEntry(player.lastKnownName, player.totalQuestsCompleted, player.maxQuestStreak, player.totalCollected, player.maxRewardStreak));
        }
        cachedQuestsBoard = new LeaderboardResponsePayload(0, questCompletionList, questStreakList);

        List<TopEntry> rewardCollectionList = new ArrayList<>();
        List<TopEntry> rewardStreakList = new ArrayList<>();

        allPlayers.sort((playerA, playerB) -> Integer.compare(playerB.getValue().totalCollected, playerA.getValue().totalCollected));
        for (int i = 0; i < allPlayers.size() && rewardCollectionList.size() < 10; i++) {
            PlayerData player = allPlayers.get(i).getValue();
            if (player.totalCollected > 0) rewardCollectionList.add(new TopEntry(player.lastKnownName, player.totalQuestsCompleted, player.maxQuestStreak, player.totalCollected, player.maxRewardStreak));
        }

        allPlayers.sort((playerA, playerB) -> Integer.compare(playerB.getValue().maxRewardStreak, playerA.getValue().maxRewardStreak));
        for (int i = 0; i < allPlayers.size() && rewardStreakList.size() < 10; i++) {
            PlayerData player = allPlayers.get(i).getValue();
            if (player.maxRewardStreak > 0) rewardStreakList.add(new TopEntry(player.lastKnownName, player.totalQuestsCompleted, player.maxQuestStreak, player.totalCollected, player.maxRewardStreak));
        }
        cachedRewardsBoard = new LeaderboardResponsePayload(1, rewardCollectionList, rewardStreakList);
    }
}