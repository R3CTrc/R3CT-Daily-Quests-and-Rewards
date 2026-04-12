package com.r3ct.quests.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RerollQuestPayload(int questIndex) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<RerollQuestPayload> ID = new CustomPacketPayload.Type<>(Identifier.parse("r3ct:reroll_quest"));

    public static final StreamCodec<FriendlyByteBuf, RerollQuestPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, RerollQuestPayload::questIndex,
            RerollQuestPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}