package com.r3ct.daily.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SubmitQuestItemPayload(int questIndex, int slotIndex) implements CustomPacketPayload {
    public static final Type<SubmitQuestItemPayload> TYPE = new Type<>(Identifier.parse("r3ct_daily:submit_quest_item"));

    public static final StreamCodec<FriendlyByteBuf, SubmitQuestItemPayload> STREAM_CODEC = StreamCodec.ofMember(
            SubmitQuestItemPayload::write,
            SubmitQuestItemPayload::new
    );

    public SubmitQuestItemPayload(FriendlyByteBuf buf) {
        this(buf.readInt(), buf.readInt());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(this.questIndex);
        buf.writeInt(this.slotIndex);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}