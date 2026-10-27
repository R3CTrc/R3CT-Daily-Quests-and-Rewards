package com.r3ct.daily.data;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class PlayerData {
    public String lastRewardDate = "";
    public int rewardDay = 1;
    public int rewardStreak = 0;
    public int totalCollected = 0;
    public String lastRewardStreakDate = "";
    public List<String> claimedRewardHistory = new ArrayList<>();
    public int availableRewardShields = 0;

    public int maxQuestStreak = 0;
    public int absoluteRewardStreak = 0;
    public int maxRewardStreak = 0;
    public String lastKnownName = Component.translatable("r3ct_daily.misc.none").getString();

    public String lastQuestDate = "";
    public int questStreak = 0;
    public String lastQuestStreakDate = "";
    public int totalQuestPoints = 0;
    public int dailyQuestsCompletedToday = 0;

    public List<String> activeQuests = new ArrayList<>();
    public List<Integer> questProgress = new ArrayList<>();
    public List<String> unlockedDimensions = new ArrayList<>();

    public int totalQuestsCompleted = 0;

    public List<Boolean> questRewardsClaimed = new ArrayList<>(List.of(false, false, false, false, false));

    public List<Integer> claimedPointRewards = new ArrayList<>();

    public List<Integer> claimedBonusRewards = new ArrayList<>();

    public int perfectDaysCount = 0;
    public int availableQuestShields = 0;

    public static final Codec<PlayerData> CODEC = CompoundTag.CODEC.xmap(PlayerData::fromNbt, PlayerData::toNbt);

    public PlayerData() {
        for(int i = 0; i < 7; i++) claimedRewardHistory.add("");
        activeQuests.clear();
        questProgress.clear();
        questRewardsClaimed.clear();
        for(int i = 0; i < 5; i++) {
            activeQuests.add("");
            questProgress.add(0);
            questRewardsClaimed.add(false);
        }
        unlockedDimensions.add("minecraft:overworld");
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("lastRewardDate", lastRewardDate != null ? lastRewardDate : "");
        nbt.putInt("rewardDay", rewardDay);
        nbt.putInt("rewardStreak", rewardStreak);
        nbt.putInt("totalCollected", totalCollected);
        nbt.putString("lastRewardStreakDate", lastRewardStreakDate != null ? lastRewardStreakDate : "");
        nbt.putInt("availableRewardShields", availableRewardShields);

        nbt.putInt("maxQuestStreak", maxQuestStreak);
        nbt.putInt("absoluteRewardStreak", absoluteRewardStreak);
        nbt.putInt("maxRewardStreak", maxRewardStreak);
        nbt.putString("lastKnownName", lastKnownName);

        nbt.putString("lastQuestDate", lastQuestDate != null ? lastQuestDate : "");
        nbt.putInt("questStreak", questStreak);
        nbt.putString("lastQuestStreakDate", lastQuestStreakDate != null ? lastQuestStreakDate : "");
        nbt.putInt("totalQuestPoints", totalQuestPoints);
        nbt.putInt("dailyQuestsCompletedToday", dailyQuestsCompletedToday);

        nbt.putInt("perfectDaysCount", perfectDaysCount);
        nbt.putInt("availableQuestShields", availableQuestShields);

        nbt.putInt("totalQuestsCompleted", totalQuestsCompleted);

        ListTag questsList = new ListTag();
        for (String questId : activeQuests) questsList.add(StringTag.valueOf(questId != null ? questId : ""));
        nbt.put("activeQuests", questsList);

        ListTag progressList = new ListTag();
        for (Integer progress : questProgress) progressList.add(IntTag.valueOf(progress));
        nbt.put("questProgress", progressList);

        ListTag historyList = new ListTag();
        for (String historyEntry : claimedRewardHistory) historyList.add(StringTag.valueOf(historyEntry != null ? historyEntry : ""));
        nbt.put("claimedRewardHistory", historyList);

        ListTag dimensionList = new ListTag();
        for (String dimensionId : unlockedDimensions) dimensionList.add(StringTag.valueOf(dimensionId != null ? dimensionId : ""));
        nbt.put("unlockedDimensions", dimensionList);

        ListTag questRewardsList = new ListTag();
        for (Boolean isClaimed : questRewardsClaimed) questRewardsList.add(IntTag.valueOf(isClaimed ? 1 : 0));
        nbt.put("questRewardsClaimed", questRewardsList);

        ListTag pointRewardsList = new ListTag();
        for (Integer pointReward : claimedPointRewards) pointRewardsList.add(IntTag.valueOf(pointReward));
        nbt.put("claimedPointRewards", pointRewardsList);

        ListTag bonusRewardsList = new ListTag();
        for (Integer bonusReward : claimedBonusRewards) bonusRewardsList.add(IntTag.valueOf(bonusReward));
        nbt.put("claimedBonusRewards", bonusRewardsList);

        return nbt;
    }

    public static PlayerData fromNbt(CompoundTag nbt) {
        PlayerData data = new PlayerData();

        data.rewardDay = nbt.getInt("rewardDay").orElse(1);
        data.rewardStreak = nbt.getInt("rewardStreak").orElse(0);
        data.totalCollected = nbt.getInt("totalCollected").orElse(0);
        data.availableRewardShields = nbt.getInt("availableRewardShields").orElse(0);

        data.questStreak = nbt.getInt("questStreak").orElse(0);
        data.totalQuestPoints = nbt.getInt("totalQuestPoints").orElse(0);
        data.dailyQuestsCompletedToday = nbt.getInt("dailyQuestsCompletedToday").orElse(0);

        data.maxQuestStreak = nbt.getInt("maxQuestStreak").orElse(0);
        data.absoluteRewardStreak = nbt.getInt("absoluteRewardStreak").orElse(0);
        data.maxRewardStreak = nbt.getInt("maxRewardStreak").orElse(0);
        data.lastKnownName = nbt.getString("lastKnownName").orElse(Component.translatable("r3ct_daily.misc.none").getString());

        data.perfectDaysCount = nbt.getInt("perfectDaysCount").orElse(0);
        data.availableQuestShields = nbt.getInt("availableQuestShields").orElse(0);

        data.totalQuestsCompleted = nbt.getInt("totalQuestsCompleted").orElse(0);

        data.lastRewardDate = nbt.getString("lastRewardDate").orElse("");
        data.lastRewardStreakDate = nbt.getString("lastRewardStreakDate").orElse("");
        data.lastQuestDate = nbt.getString("lastQuestDate").orElse("");
        data.lastQuestStreakDate = nbt.getString("lastQuestStreakDate").orElse("");

        if (nbt.contains("activeQuests")) {
            data.activeQuests.clear();
            if (nbt.get("activeQuests") instanceof ListTag list) {
                for (int i = 0; i < list.size(); i++) {
                    data.activeQuests.add(list.getString(i).orElse(""));
                }
            }
        }

        if (nbt.contains("questProgress")) {
            data.questProgress.clear();
            if (nbt.get("questProgress") instanceof ListTag list) {
                for (int i = 0; i < list.size(); i++) {
                    data.questProgress.add(list.getInt(i).orElse(0));
                }
            }
        }

        if (nbt.contains("claimedRewardHistory")) {
            data.claimedRewardHistory.clear();
            if (nbt.get("claimedRewardHistory") instanceof ListTag list) {
                for (int i = 0; i < list.size(); i++) {
                    data.claimedRewardHistory.add(list.getString(i).orElse(""));
                }
            }
        }

        if (nbt.contains("unlockedDimensions")) {
            data.unlockedDimensions.clear();
            if (nbt.get("unlockedDimensions") instanceof ListTag list) {
                for (int i = 0; i < list.size(); i++) {
                    data.unlockedDimensions.add(list.getString(i).orElse(""));
                }
            }
        }

        if (nbt.contains("questRewardsClaimed")) {
            data.questRewardsClaimed.clear();
            if (nbt.get("questRewardsClaimed") instanceof ListTag list) {
                for (int i = 0; i < list.size(); i++) {
                    data.questRewardsClaimed.add(list.getInt(i).orElse(0) == 1);
                }
            }
        }

        if (nbt.contains("claimedPointRewards")) {
            data.claimedPointRewards.clear();
            if (nbt.get("claimedPointRewards") instanceof ListTag list) {
                for (int i = 0; i < list.size(); i++) {
                    data.claimedPointRewards.add(list.getInt(i).orElse(0));
                }
            }
        }

        if (nbt.contains("claimedBonusRewards")) {
            data.claimedBonusRewards.clear();
            if (nbt.get("claimedBonusRewards") instanceof ListTag list) {
                for (int i = 0; i < list.size(); i++) {
                    data.claimedBonusRewards.add(list.getInt(i).orElse(0));
                }
            }
        }

        while (data.questRewardsClaimed.size() < 5) {
            data.questRewardsClaimed.add(false);
        }

        return data;
    }
}