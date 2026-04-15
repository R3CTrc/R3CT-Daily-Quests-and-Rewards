package com.r3ct.quests.network;

import com.r3ct.quests.Constants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import java.util.ArrayList;
import java.util.List;

public record OpenRewardsPayload(
        int rewardDay,
        String lastRewardDate,
        int streak,
        int totalCollected,
        List<String> claimedRewardHistory,
        int availableRewardFreezes,
        List<Integer> claimedBonusRewards,
        int maxRewardShields,
        int questRefreshHour
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenRewardsPayload> ID =
            new CustomPacketPayload.Type<>(net.minecraft.resources.Identifier.parse(Constants.MOD_ID + ":open_rewards"));

    public static final StreamCodec<FriendlyByteBuf, OpenRewardsPayload> CODEC = CustomPacketPayload.codec(
            OpenRewardsPayload::write,
            OpenRewardsPayload::new
    );

    public OpenRewardsPayload(FriendlyByteBuf buf) {
        this(
                buf.readInt(),
                buf.readUtf(),
                buf.readInt(),
                buf.readInt(),
                buf.readCollection(ArrayList::new, FriendlyByteBuf::readUtf),
                buf.readInt(),
                buf.readCollection(ArrayList::new, FriendlyByteBuf::readInt),
                buf.readInt(),
                buf.readInt()
        );
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(rewardDay);
        buf.writeUtf(lastRewardDate);
        buf.writeInt(streak);
        buf.writeInt(totalCollected);
        buf.writeCollection(claimedRewardHistory, FriendlyByteBuf::writeUtf);
        buf.writeInt(availableRewardFreezes);
        buf.writeCollection(claimedBonusRewards, FriendlyByteBuf::writeInt);
        buf.writeInt(maxRewardShields);
        buf.writeInt(questRefreshHour);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}