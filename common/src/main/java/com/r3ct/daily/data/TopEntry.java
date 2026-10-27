package com.r3ct.daily.data;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record TopEntry(String name, int totalQuests, int maxQuestStreak, int totalRewards, int maxRewardStreak) {

    public static final StreamCodec<FriendlyByteBuf, TopEntry> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(32767), TopEntry::name,
            ByteBufCodecs.INT, TopEntry::totalQuests,
            ByteBufCodecs.INT, TopEntry::maxQuestStreak,
            ByteBufCodecs.INT, TopEntry::totalRewards,
            ByteBufCodecs.INT, TopEntry::maxRewardStreak,
            TopEntry::new
    ).cast();

    public static void write(FriendlyByteBuf buffer, TopEntry leaderboardEntry) {
        STREAM_CODEC.encode(buffer, leaderboardEntry);
    }

    public static TopEntry read(FriendlyByteBuf buffer) {
        return STREAM_CODEC.decode(buffer);
    }
}