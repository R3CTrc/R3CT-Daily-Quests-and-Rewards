package com.r3ct.quests;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RequestLeaderboardPayload(int boardType) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<RequestLeaderboardPayload> ID = new CustomPacketPayload.Type<>(Identifier.parse("r3ct:req_leaderboard"));

    public static final StreamCodec<FriendlyByteBuf, RequestLeaderboardPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, RequestLeaderboardPayload::boardType,
            RequestLeaderboardPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return ID; }
}