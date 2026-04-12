package com.r3ct.quests.network;

import com.r3ct.quests.Constants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
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
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncQuestsPayload> ID =
            new CustomPacketPayload.Type<>(net.minecraft.resources.Identifier.parse(Constants.MOD_ID + ":sync_quests"));

    public static final StreamCodec<FriendlyByteBuf, SyncQuestsPayload> CODEC = CustomPacketPayload.codec(
            SyncQuestsPayload::write,
            SyncQuestsPayload::new
    );

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
        buf.writeInt(questStreak);
        buf.writeInt(totalQuestPoints);
        buf.writeInt(dailyQuestsCompletedToday);
        buf.writeCollection(activeQuests, FriendlyByteBuf::writeUtf);
        buf.writeCollection(questProgress, FriendlyByteBuf::writeInt);
        buf.writeInt(streak);
        buf.writeInt(perfectDaysCount);
        buf.writeInt(availableFreezes);
        buf.writeInt(availableRewardFreezes);
        buf.writeCollection(questRewardsClaimed, FriendlyByteBuf::writeBoolean);
        buf.writeCollection(claimedPointRewards, FriendlyByteBuf::writeInt);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}