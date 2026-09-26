package com.r3ct.daily.network;

import com.r3ct.daily.Constants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record SyncQuestsPayload(
        int questStreak,
        int totalQuestPoints,
        int dailyQuestsCompletedToday,
        List<String> activeQuests,
        List<Integer> questProgress,
        int streak,
        int perfectDaysCount,
        int availableFreezes,
        int availableRewardFreezes,
        List<Boolean> questRewardsClaimed,
        List<Integer> claimedPointRewards
) {

    public static final ResourceLocation ID = new ResourceLocation(Constants.MOD_ID, "sync_quests");

    public SyncQuestsPayload(FriendlyByteBuf buf) {
        this(
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readCollection(ArrayList::new, FriendlyByteBuf::readUtf),
                buf.readCollection(ArrayList::new, FriendlyByteBuf::readInt),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readCollection(ArrayList::new, FriendlyByteBuf::readBoolean),
                buf.readCollection(ArrayList::new, FriendlyByteBuf::readInt)
        );
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(this.questStreak);
        buf.writeInt(this.totalQuestPoints);
        buf.writeInt(this.dailyQuestsCompletedToday);
        buf.writeCollection(this.activeQuests, FriendlyByteBuf::writeUtf);
        buf.writeCollection(this.questProgress, FriendlyByteBuf::writeInt);
        buf.writeInt(this.streak);
        buf.writeInt(this.perfectDaysCount);
        buf.writeInt(this.availableFreezes);
        buf.writeInt(this.availableRewardFreezes);
        buf.writeCollection(this.questRewardsClaimed, FriendlyByteBuf::writeBoolean);
        buf.writeCollection(this.claimedPointRewards, FriendlyByteBuf::writeInt);
    }
}