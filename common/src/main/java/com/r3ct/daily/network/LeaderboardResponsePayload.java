package com.r3ct.daily.network;

import com.r3ct.daily.data.TopEntry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

public record LeaderboardResponsePayload(int boardType, List<TopEntry> leftList, List<TopEntry> rightList) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<LeaderboardResponsePayload> ID = new CustomPacketPayload.Type<>(Identifier.parse("r3ct_daily:leaderboard_res"));

    public static final StreamCodec<FriendlyByteBuf, LeaderboardResponsePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, LeaderboardResponsePayload::boardType,
            TopEntry.STREAM_CODEC.apply(ByteBufCodecs.list()), LeaderboardResponsePayload::leftList,
            TopEntry.STREAM_CODEC.apply(ByteBufCodecs.list()), LeaderboardResponsePayload::rightList,
            LeaderboardResponsePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return ID; }
}