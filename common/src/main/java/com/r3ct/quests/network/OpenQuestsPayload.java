package com.r3ct.quests.network;

import com.r3ct.quests.Constants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
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
        int maxStoredQuestShields
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenQuestsPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.parse(Constants.MOD_ID + ":open_quests"));

    public static final StreamCodec<FriendlyByteBuf, OpenQuestsPayload> CODEC = CustomPacketPayload.codec(
            OpenQuestsPayload::write,
            OpenQuestsPayload::new
    );

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
                buf.readInt()
        );
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(questStreak);
        buf.writeInt(totalQuestPoints);
        buf.writeInt(dailyQuestsCompletedToday);
        buf.writeCollection(activeQuests, FriendlyByteBuf::writeUtf);
        buf.writeCollection(questProgress, FriendlyByteBuf::writeInt);
        buf.writeInt(streak);
        buf.writeInt(perfectDaysCount);
        buf.writeInt(availableFreezes);
        buf.writeCollection(questRewardsClaimed, FriendlyByteBuf::writeBoolean);
        buf.writeCollection(claimedPointRewards, FriendlyByteBuf::writeInt);
        buf.writeBoolean(enableQuestRerolling);
        buf.writeInt(rerollCostEasy);
        buf.writeInt(rerollCostMedium);
        buf.writeInt(rerollCostHard);
        buf.writeInt(xpDailyReward);
        buf.writeInt(xpPerQuestEasy);
        buf.writeInt(xpPerQuestMedium);
        buf.writeInt(xpPerQuestHard);
        buf.writeInt(perfectDaysForShield);
        buf.writeInt(maxStoredQuestShields);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return ID; }
}