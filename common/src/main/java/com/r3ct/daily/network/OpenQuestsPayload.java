package com.r3ct.daily.network;

import com.r3ct.daily.Constants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import java.util.ArrayList;
import java.util.List;

public record OpenQuestsPayload(
        int questStreak,
        int totalQuestPoints,
        int dailyQuestsCompletedToday,
        List<String> activeQuests,
        List<Integer> questProgress,
        int streak,
        int perfectDaysCount,
        int availableFreezes,
        List<Boolean> questRewardsClaimed,
        List<Integer> claimedPointRewards,
        boolean enableQuestRerolling,
        int rerollCostEasy,
        int rerollCostMedium,
        int rerollCostHard,
        int xpDailyReward,
        int xpPerQuestEasy,
        int xpPerQuestMedium,
        int xpPerQuestHard,
        int perfectDaysForShield,
        int maxStoredQuestShields,
        float questStreakXpMultiplier
) {

    public static final ResourceLocation ID = new ResourceLocation(Constants.MOD_ID, "open_quests");

    public OpenQuestsPayload(FriendlyByteBuf buf) {
        this(
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readCollection(ArrayList::new, FriendlyByteBuf::readUtf),
                buf.readCollection(ArrayList::new, FriendlyByteBuf::readInt),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readCollection(ArrayList::new, FriendlyByteBuf::readBoolean),
                buf.readCollection(ArrayList::new, FriendlyByteBuf::readInt),
                buf.readBoolean(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readFloat()
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
        buf.writeCollection(this.questRewardsClaimed, FriendlyByteBuf::writeBoolean);
        buf.writeCollection(this.claimedPointRewards, FriendlyByteBuf::writeInt);
        buf.writeBoolean(this.enableQuestRerolling);
        buf.writeInt(this.rerollCostEasy);
        buf.writeInt(this.rerollCostMedium);
        buf.writeInt(this.rerollCostHard);
        buf.writeInt(this.xpDailyReward);
        buf.writeInt(this.xpPerQuestEasy);
        buf.writeInt(this.xpPerQuestMedium);
        buf.writeInt(this.xpPerQuestHard);
        buf.writeInt(this.perfectDaysForShield);
        buf.writeInt(this.maxStoredQuestShields);
        buf.writeFloat(this.questStreakXpMultiplier);
    }
}