package com.r3ct.daily.network;

import com.r3ct.daily.Constants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

public record OpenRewardsPayload(
        int rewardDay,
        String lastRewardDate,
        int streak,
        int absoluteRewardStreak,
        int totalCollected,
        List<String> claimedRewardHistory,
        int availableRewardFreezes,
        List<Integer> claimedBonusRewards,
        int maxRewardShields,
        int questRefreshHour
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenRewardsPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.parse(Constants.MOD_ID + ":open_rewards"));

    public static final StreamCodec<FriendlyByteBuf, OpenRewardsPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, OpenRewardsPayload::rewardDay,
            ByteBufCodecs.stringUtf8(32767), OpenRewardsPayload::lastRewardDate,
            ByteBufCodecs.INT, OpenRewardsPayload::streak,
            ByteBufCodecs.INT, OpenRewardsPayload::absoluteRewardStreak,
            ByteBufCodecs.INT, OpenRewardsPayload::totalCollected,
            ByteBufCodecs.stringUtf8(32767).apply(ByteBufCodecs.list()), OpenRewardsPayload::claimedRewardHistory,
            ByteBufCodecs.INT, OpenRewardsPayload::availableRewardFreezes,
            ByteBufCodecs.INT.apply(ByteBufCodecs.list()), OpenRewardsPayload::claimedBonusRewards,
            ByteBufCodecs.INT, OpenRewardsPayload::maxRewardShields,
            ByteBufCodecs.INT, OpenRewardsPayload::questRefreshHour,
            OpenRewardsPayload::new
    ).cast();

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}