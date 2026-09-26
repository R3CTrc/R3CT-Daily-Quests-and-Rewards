package com.r3ct.daily.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record SubmitQuestItemPayload(int questIndex, int slotIndex) {
    public static final ResourceLocation TYPE = new ResourceLocation("r3ct_daily", "submit_quest_item");

    public SubmitQuestItemPayload(FriendlyByteBuf buf) {
        this(buf.readInt(), buf.readInt());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(this.questIndex);
        buf.writeInt(this.slotIndex);
    }
}