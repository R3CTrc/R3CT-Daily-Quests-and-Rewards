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
    public int streak = 0;
    public int totalCollected = 0;
    public String lastStreakDate = "";
    public List<String> claimedRewardHistory = new ArrayList<>();
    public int availableRewardFreezes = 0;

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
    public int availableFreezes = 0;

    public static final Codec<PlayerData> CODEC = CompoundTag.CODEC.xmap(PlayerData::fromNbt, PlayerData::toNbt);

    public PlayerData() {
        for (int i = 0; i < 7; i++) claimedRewardHistory.add("");
        activeQuests.clear();
        questProgress.clear();
        questRewardsClaimed.clear();
        for (int i = 0; i < 5; i++) {
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
        nbt.putInt("streak", streak);
        nbt.putInt("totalCollected", totalCollected);
        nbt.putString("lastStreakDate", lastStreakDate != null ? lastStreakDate : "");
        nbt.putInt("availableRewardFreezes", availableRewardFreezes);

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
        nbt.putInt("availableFreezes", availableFreezes);

        nbt.putInt("totalQuestsCompleted", totalQuestsCompleted);

        ListTag quests = new ListTag();
        for (String q : activeQuests) quests.add(StringTag.valueOf(q != null ? q : ""));
        nbt.put("activeQuests", quests);

        ListTag progress = new ListTag();
        for (Integer p : questProgress) progress.add(IntTag.valueOf(p));
        nbt.put("questProgress", progress);

        ListTag history = new ListTag();
        for (String h : claimedRewardHistory) history.add(StringTag.valueOf(h != null ? h : ""));
        nbt.put("claimedRewardHistory", history);

        ListTag dims = new ListTag();
        for (String d : unlockedDimensions) dims.add(StringTag.valueOf(d != null ? d : ""));
        nbt.put("unlockedDimensions", dims);

        ListTag questRewards = new ListTag();
        for (Boolean claimed : questRewardsClaimed) questRewards.add(IntTag.valueOf(claimed ? 1 : 0));
        nbt.put("questRewardsClaimed", questRewards);

        ListTag pointRewards = new ListTag();
        for (Integer pt : claimedPointRewards) pointRewards.add(IntTag.valueOf(pt));
        nbt.put("claimedPointRewards", pointRewards);

        ListTag bonusRewards = new ListTag();
        for (Integer br : claimedBonusRewards) bonusRewards.add(IntTag.valueOf(br));
        nbt.put("claimedBonusRewards", bonusRewards);

        return nbt;
    }

    public static PlayerData fromNbt(CompoundTag nbt) {
        PlayerData data = new PlayerData();

        data.rewardDay = nbt.contains("rewardDay") ? nbt.getInt("rewardDay") : 1;

        data.streak = nbt.getInt("streak");
        data.totalCollected = nbt.getInt("totalCollected");
        data.availableRewardFreezes = nbt.getInt("availableRewardFreezes");

        data.questStreak = nbt.getInt("questStreak");
        data.totalQuestPoints = nbt.getInt("totalQuestPoints");
        data.dailyQuestsCompletedToday = nbt.getInt("dailyQuestsCompletedToday");

        data.maxQuestStreak = nbt.getInt("maxQuestStreak");
        data.absoluteRewardStreak = nbt.getInt("absoluteRewardStreak");
        data.maxRewardStreak = nbt.getInt("maxRewardStreak");

        data.lastKnownName = nbt.contains("lastKnownName") ? nbt.getString("lastKnownName") : Component.translatable("r3ct_daily.misc.none").getString();

        data.perfectDaysCount = nbt.getInt("perfectDaysCount");
        data.availableFreezes = nbt.getInt("availableFreezes");

        data.totalQuestsCompleted = nbt.getInt("totalQuestsCompleted");

        data.lastRewardDate = nbt.getString("lastRewardDate");
        data.lastStreakDate = nbt.getString("lastStreakDate");
        data.lastQuestDate = nbt.getString("lastQuestDate");
        data.lastQuestStreakDate = nbt.getString("lastQuestStreakDate");

        if (nbt.contains("activeQuests")) {
            data.activeQuests.clear();
            if (nbt.get("activeQuests") instanceof ListTag list) {
                for (int i = 0; i < list.size(); i++) {
                    data.activeQuests.add(list.getString(i));
                }
            }
        }

        if (nbt.contains("questProgress")) {
            data.questProgress.clear();
            if (nbt.get("questProgress") instanceof ListTag list) {
                for (int i = 0; i < list.size(); i++) {
                    data.questProgress.add(list.getInt(i));
                }
            }
        }

        if (nbt.contains("claimedRewardHistory")) {
            data.claimedRewardHistory.clear();
            if (nbt.get("claimedRewardHistory") instanceof ListTag list) {
                for (int i = 0; i < list.size(); i++) {
                    data.claimedRewardHistory.add(list.getString(i));
                }
            }
        }

        if (nbt.contains("unlockedDimensions")) {
            data.unlockedDimensions.clear();
            if (nbt.get("unlockedDimensions") instanceof ListTag list) {
                for (int i = 0; i < list.size(); i++) {
                    data.unlockedDimensions.add(list.getString(i));
                }
            }
        }

        if (nbt.contains("questRewardsClaimed")) {
            data.questRewardsClaimed.clear();
            if (nbt.get("questRewardsClaimed") instanceof ListTag list) {
                for (int i = 0; i < list.size(); i++) {
                    data.questRewardsClaimed.add(list.getInt(i) == 1);
                }
            }
        }

        if (nbt.contains("claimedPointRewards")) {
            data.claimedPointRewards.clear();
            if (nbt.get("claimedPointRewards") instanceof ListTag list) {
                for (int i = 0; i < list.size(); i++) {
                    data.claimedPointRewards.add(list.getInt(i));
                }
            }
        }

        if (nbt.contains("claimedBonusRewards")) {
            data.claimedBonusRewards.clear();
            if (nbt.get("claimedBonusRewards") instanceof ListTag list) {
                for (int i = 0; i < list.size(); i++) {
                    data.claimedBonusRewards.add(list.getInt(i));
                }
            }
        }

        while (data.questRewardsClaimed.size() < 5) {
            data.questRewardsClaimed.add(false);
        }

        return data;
    }
}