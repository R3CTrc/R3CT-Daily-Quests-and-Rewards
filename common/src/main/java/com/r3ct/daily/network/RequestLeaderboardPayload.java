package com.r3ct.daily.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record RequestLeaderboardPayload(int boardType) {
    public static final ResourceLocation ID = new ResourceLocation("r3ct_daily", "req_leaderboard");

    public RequestLeaderboardPayload(FriendlyByteBuf buf) {
        this(buf.readInt());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(this.boardType);
    }
}