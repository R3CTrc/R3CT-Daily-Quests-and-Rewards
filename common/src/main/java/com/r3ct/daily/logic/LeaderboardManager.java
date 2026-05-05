package com.r3ct.daily.logic;

import com.r3ct.daily.data.ModState;
import com.r3ct.daily.data.PlayerData;
import com.r3ct.daily.data.TopEntry;
import com.r3ct.daily.network.LeaderboardResponsePayload;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LeaderboardManager {
    public static LeaderboardResponsePayload cachedQuestsBoard = null;
    public static LeaderboardResponsePayload cachedRewardsBoard = null;
    public static int lastLeaderboardUpdateTick = -1;

    public static void updateLeaderboardCache(MinecraftServer server) {
        ModState state = ModState.get(server);
        List<Map.Entry<java.util.UUID, PlayerData>> allPlayers = new ArrayList<>(state.players.entrySet());

        List<TopEntry> leftQ = new ArrayList<>();
        List<TopEntry> rightQ = new ArrayList<>();

        allPlayers.sort((a, b) -> Integer.compare(b.getValue().totalQuestsCompleted, a.getValue().totalQuestsCompleted));
        for (int i = 0; i < allPlayers.size() && leftQ.size() < 10; i++) {
            PlayerData p = allPlayers.get(i).getValue();
            if (p.totalQuestsCompleted > 0) leftQ.add(new TopEntry(p.lastKnownName, p.totalQuestsCompleted, p.maxQuestStreak, p.totalCollected, p.maxRewardStreak));
        }

        allPlayers.sort((a, b) -> Integer.compare(b.getValue().maxQuestStreak, a.getValue().maxQuestStreak));
        for (int i = 0; i < allPlayers.size() && rightQ.size() < 10; i++) {
            PlayerData p = allPlayers.get(i).getValue();
            if (p.maxQuestStreak > 0) rightQ.add(new TopEntry(p.lastKnownName, p.totalQuestsCompleted, p.maxQuestStreak, p.totalCollected, p.maxRewardStreak));
        }
        cachedQuestsBoard = new LeaderboardResponsePayload(0, leftQ, rightQ);

        List<TopEntry> leftR = new ArrayList<>();
        List<TopEntry> rightR = new ArrayList<>();

        allPlayers.sort((a, b) -> Integer.compare(b.getValue().totalCollected, a.getValue().totalCollected));
        for (int i = 0; i < allPlayers.size() && leftR.size() < 10; i++) {
            PlayerData p = allPlayers.get(i).getValue();
            if (p.totalCollected > 0) leftR.add(new TopEntry(p.lastKnownName, p.totalQuestsCompleted, p.maxQuestStreak, p.totalCollected, p.maxRewardStreak));
        }

        allPlayers.sort((a, b) -> Integer.compare(b.getValue().maxRewardStreak, a.getValue().maxRewardStreak));
        for (int i = 0; i < allPlayers.size() && rightR.size() < 10; i++) {
            PlayerData p = allPlayers.get(i).getValue();
            if (p.maxRewardStreak > 0) rightR.add(new TopEntry(p.lastKnownName, p.totalQuestsCompleted, p.maxQuestStreak, p.totalCollected, p.maxRewardStreak));
        }
        cachedRewardsBoard = new LeaderboardResponsePayload(1, leftR, rightR);
    }
}