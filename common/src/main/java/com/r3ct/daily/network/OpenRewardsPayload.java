package com.r3ct.daily.network;

import com.r3ct.daily.Constants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

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
) {

    public static final ResourceLocation ID = new ResourceLocation(Constants.MOD_ID, "open_rewards");

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
        buf.writeInt(this.rewardDay);
        buf.writeUtf(this.lastRewardDate);
        buf.writeInt(this.streak);
        buf.writeInt(this.totalCollected);
        buf.writeCollection(this.claimedRewardHistory, FriendlyByteBuf::writeUtf);
        buf.writeInt(this.availableRewardFreezes);
        buf.writeCollection(this.claimedBonusRewards, FriendlyByteBuf::writeInt);
        buf.writeInt(this.maxRewardShields);
        buf.writeInt(this.questRefreshHour);
    }
}