package com.r3ct.quests;

import net.minecraft.network.FriendlyByteBuf;

public record TopEntry(String name, int totalQuests, int maxQuestStreak, int totalRewards, int maxRewardStreak) {
    public static void write(FriendlyByteBuf buf, TopEntry entry) {
        buf.writeUtf(entry.name());
        buf.writeInt(entry.totalQuests());
        buf.writeInt(entry.maxQuestStreak());
        buf.writeInt(entry.totalRewards());
        buf.writeInt(entry.maxRewardStreak());
    }
    public static TopEntry read(FriendlyByteBuf buf) {
        return new TopEntry(buf.readUtf(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt());
    }
}