package com.r3ct.daily.network;

import com.r3ct.daily.Constants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

public record SyncQuestsPayload(
        int questStreak,
        int totalQuestPoints,
        int dailyQuestsCompletedToday,
        List<String> activeQuests,
        List<Integer> questProgress,
        int rewardStreak,
        int perfectDaysCount,
        int availableQuestShields,
        int availableRewardShields,
        List<Boolean> questRewardsClaimed,
        List<Integer> claimedPointRewards
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncQuestsPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.parse(Constants.MOD_ID + ":sync_quests"));

    public static final StreamCodec<FriendlyByteBuf, SyncQuestsPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, SyncQuestsPayload::questStreak,
            ByteBufCodecs.INT, SyncQuestsPayload::totalQuestPoints,
            ByteBufCodecs.INT, SyncQuestsPayload::dailyQuestsCompletedToday,
            ByteBufCodecs.stringUtf8(32767).apply(ByteBufCodecs.list()), SyncQuestsPayload::activeQuests,
            ByteBufCodecs.INT.apply(ByteBufCodecs.list()), SyncQuestsPayload::questProgress,
            ByteBufCodecs.INT, SyncQuestsPayload::rewardStreak,
            ByteBufCodecs.INT, SyncQuestsPayload::perfectDaysCount,
            ByteBufCodecs.INT, SyncQuestsPayload::availableQuestShields,
            ByteBufCodecs.INT, SyncQuestsPayload::availableRewardShields,
            ByteBufCodecs.BOOL.apply(ByteBufCodecs.list()), SyncQuestsPayload::questRewardsClaimed,
            ByteBufCodecs.INT.apply(ByteBufCodecs.list()), SyncQuestsPayload::claimedPointRewards,
            SyncQuestsPayload::new
    ).cast();

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}